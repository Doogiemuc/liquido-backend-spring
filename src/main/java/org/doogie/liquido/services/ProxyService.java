package org.doogie.liquido.services;

import jdk.nashorn.internal.parser.Token;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProxyService {

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	UserRepo userRepo;

	@Autowired
	TokenChecksumRepo checksumRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	BallotRepo ballotRepo;

	/**
	 * Get all proxies that a voter has asigned.
	 * @param fromUser the voter
	 * @return a map with one entry per area where a direct proxy is assigned
	 */
	public Map<AreaModel, UserModel> getProxyMap(UserModel fromUser) {
		return delegationRepo.findByFromUser(fromUser).stream().collect(Collectors.toMap(delegation -> delegation.getArea(), delegation -> delegation.getToProxy()));
		/* This one liner was formerly known as this ... beginning to like Java8 streams :-)
		List<DelegationModel> delegations = delegationRepo.findByFromUser(fromUser);
		Map<AreaModel, UserModel> proxyMap = new HashMap<>();
		for (DelegationModel delegation : delegations) {
			proxyMap.put(delegation.getArea(), delegation.getToProxy());
		}
		return proxyMap;
		*/
   }


	/* used for testing. normally proxy should be assigned with a valid voterToken
	public DelegationModel assignProxyWithPassword(AreaModel area, UserModel fromUser, UserModel toProxy, String fromUserPasswordHash) throws LiquidoException {
		String voterToken		 = castVoteService.createVoterToken(fromUser, area);
		return this.assignProxy(area, fromUser, toProxy, voterToken);
	}
	*/

	/**
	 * User forwards his right to vote to a proxy in one area.
	 *
	 * After some sanity checks, this methods checks for a circular delegation which would be forbidden.
	 * If toProxy is a public proxy, then we can directly delegate user's checksum to the public proxie's checksum.
	 * Otherwise we have to store a task, so that the proxy needs to accept the delegation.
	 *
	 * @param area area in which to assign the proxy
	 * @param fromUser voter that forwards his right to vote
	 * @param toProxy proxy that receives the right and thus can vote in place of fromUser
	 * @param voterToken user's voterToken, so that we calculate his checksum. BE CAREFULL, DO NOT PASS a password
	 * @return the newly created or updated DelegationModel or null, when the proxy is not public and must still confirm the delegation
	 * @throws LiquidoException when the assignment would create a circular proxy chain or when voterToken is invalid
	 */
	@Transactional
	public DelegationModel assignProxy(AreaModel area, UserModel fromUser, UserModel toProxy, String voterToken) throws LiquidoException {
		log.trace("assignProxy("+area+", fromUser="+fromUser+", toProxy="+toProxy+")");
		//----- sanity checks
		if (area == null || fromUser == null || toProxy == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Need area, fromUser and toProxy!");
		if (fromUser.equals(toProxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot delegate to myself!");

		//----- check for circular delegation, which cannot be allowed
		if (thisWouldBeCircularDelegation(area, fromUser, toProxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");

		//----- validate voterToken and get voters checksumModel, so that we can delegate it anonymously.
		TokenChecksumModel voterChecksumModel = castVoteService.isVoterTokenValid(voterToken);
		if (voterChecksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: voterToken seems to be invalid. Cannot not find its checksum.");

		//----- upsert delegation from user to proxy in that area
		DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		DelegationModel savedDelegation;
		if (existingDelegation == null) {
			DelegationModel delegation = new DelegationModel(area, fromUser, toProxy);
			savedDelegation = delegationRepo.save(delegation);
		} else {
			existingDelegation.setToProxy(toProxy);													// this may overwrite any previous assignment
			savedDelegation = delegationRepo.save(existingDelegation);
		}
		// implementation note: some more interesting views un upsert in JPA: http://dkublik.github.io/persisting-natural-key-entities-with-spring-data-jpa/

		//----- IF proxy has a public checksum THEN delegate to it ELSE ass a task, that proxy must confirm the delegation
		//TokenChecksumModel proxyChecksumModel = this.getChecksumModelOfPublicProxy(area, toProxy);

		//TODO: is it ok that I use the proxies password to create a voter token for him when assigning a proxy?
		String proxyToken = castVoteService.createVoterToken(toProxy, area, toProxy.getPasswordHash());
		TokenChecksumModel proxyChecksumModel = castVoteService.isVoterTokenValid(proxyToken);

		if (voterChecksumModel.equals(proxyChecksumModel))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot delegate checksum to itself!");

		if (proxyChecksumModel == null) {
			voterChecksumModel.setDelegatedTo(null);		//BUGFIX: need to null it when there was a previous assignment
			//TODO: !!! store a task, that proxy must confirm the delegation.  TASK := delegation fromUser->toProxy exists  but fromUserChecksumModel.delegatedTo == null
			log.warn("NOT YET IMPLEMENTED: store a task, that proxy must confirm the delegation. ");
			return null;
		} else {
			voterChecksumModel.setDelegatedTo(proxyChecksumModel);
			checksumRepo.save(voterChecksumModel);
			return savedDelegation;
		}
	}

	/**
	 * Proxy delegations must not be circular. A user must not delegate to a proxy, which already delegated his right
	 * to vote to himself. The proxy delegations must form a tree.
	 * @param area area for delegation
	 * @param user the current node in the tree that we check. Starting at the user that want's to assign a proxy
	 * @param proxyToCheck the new proxy that the user want's to assign
	 * @return true if proxyToCheck is not yet contained in the delegation tree below user.
	 *         false if proxyToCheck already (maybe transitively) delegated his vote to user.
	 */
	public boolean thisWouldBeCircularDelegation(AreaModel area, UserModel user, UserModel proxyToCheck) {
		List<DelegationModel> delegations = delegationRepo.findByAreaAndToProxy(area, user);   // find delegations to fromUser where fromUser is the proxy.
		for(DelegationModel delegation : delegations) {
			if (delegation.getFromUser().equals(proxyToCheck)) return true;
			if (thisWouldBeCircularDelegation(area, delegation.getFromUser(), proxyToCheck)) return true;
		}
		return false;
	}


	/**    DEPRECATED
	 * When a user opts-in to become a public proxy, then his username is stored to together with his checksum,
	 * so that other users can delegate their right to vote to this "public" checksum.
	 * @param publicProxy the voter that wants to become a public proxy
	 * @param area voter is a proxy in an area
	 * @param proxyVoterToken the voters token
	 * @return the proxy's public checksum
	 * @throws LiquidoException when voterToken is invalid

	@Transactional
	public TokenChecksumModel becomePublicProxy(UserModel publicProxy, AreaModel area, String proxyVoterToken) throws LiquidoException {
		log.trace("becomePublicProxy("+publicProxy+", "+area+")");
		TokenChecksumModel publicProxyChecksum = castVoteService.isVoterTokenValid(proxyVoterToken);
		if (publicProxyChecksum == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot become public proxy. Voter token seems to be invalid!");
		publicProxyChecksum.setPublicProxy(publicProxy);
		return checksumRepo.save(publicProxyChecksum);
	}
	*/

	/**
	 * When a user is a public proxy, then his checksum is stored together with his username. That way
	 * users can delegate their votes to this proxy immideately without the need that the proxy must confirm the delegation.
	 *
	 * Side remark: The returned checksumModel might in turn already be delegated to another 2nd level proxy.
	 *
	 * @param proxy the public proxy someone want's to delegate to
	 * @return the checksumModel of this proxy

	public TokenChecksumModel getChecksumModelOfPublicProxy(AreaModel area, UserModel proxy) {
		return checksumRepo.findByAreaAndPublicProxy(area, proxy);
	}
	*/


	/**
	 * When a user wants to check how his direct proxy has  voted for him.
	 * @param poll a poll
	 * @param voterChecksum the voter's checksum
	 * @return the ballot of the vote's direct proxy in this poll.
	 * @throws LiquidoException when this voter did not delegate his checksum to any proxy in this area
	 */
	public BallotModel getBallotOfDirectProxy(PollModel poll, TokenChecksumModel voterChecksum) throws LiquidoException {
		if (voterChecksum.getDelegatedTo() == null)
			throw new LiquidoException(LiquidoException.Errors.NO_DELEGATION, "You did not delegate your vote");
		BallotModel proxyBallot = ballotRepo.findByPollAndChecksum(poll, voterChecksum.getDelegatedTo());
		return proxyBallot;
	}

	public BallotModel getBallotOfTopProxy(PollModel poll, TokenChecksumModel voterChecksum) throws LiquidoException {
		if (voterChecksum.getDelegatedTo() == null)
			throw new LiquidoException(LiquidoException.Errors.NO_DELEGATION, "You did not delegate your vote");
		TokenChecksumModel topChecksum = findTopChecksum(voterChecksum);
		BallotModel proxyBallot = ballotRepo.findByPollAndChecksum(poll, topChecksum);
		return proxyBallot;
	}

	TokenChecksumModel findTopChecksum(TokenChecksumModel checksum) {
		if (checksum.getDelegatedTo() == null) return checksum;
		return findTopChecksum(checksum.getDelegatedTo());
	}


	/**
	 * Recursively find the "transitive" top proxy at the top of the delegation chain for this voter
	 * @param area the area of the delegation
	 * @param voter a voter that may have delegated his right to vote to a proxy
	 * @return the proxy at the top of the delegation chain starting at voter
	 *         or the voter himself if he didn't delegate to a proxy in this area.
	 */
	public UserModel findTopProxy(AreaModel area, UserModel voter) {
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(area, voter);
		if (delegation == null) return voter;
		return findTopProxy(area, delegation.getToProxy());
	}



	/**
	 * Delete the delegation from this user to his proxy in this area. Will also remove the delegateTo
	 * from the user's ChecksumModel.
	 * @param area the area of the delegation
	 * @param fromUser the user that delegated his right to vote to a proxy
	 * @param voterToken valid token of fromUser. We need this to remove the delegateTo from the anonymous TokenChecksumModel
	 * @throws LiquidoException
	 */
	@Transactional
	public void removeProxy(AreaModel area, UserModel fromUser, String voterToken) throws LiquidoException {
		log.debug("removeProxy("+area+", fromUser="+fromUser+")");
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		TokenChecksumModel checksumModel = castVoteService.isVoterTokenValid(voterToken);

		if (delegation != null) delegationRepo.delete(delegation);
		if (checksumModel != null) {
			checksumModel.setDelegatedTo(null);
			checksumRepo.save(checksumModel);
		}
	}

	/**
	 * Get the number of votes a proxy may cast because of (transitive) delegations to him. Including his own vote.
	 * @param area area of proxy
	 * @param proxy a voter that is a proxy
	 * @return the number of votes this proxy may cast, including his own one.
	 */
	public int getNumVotes(AreaModel area, UserModel proxy) {
		int numVotes = 1;
		for (DelegationModel delegation : delegationRepo.findByAreaAndToProxy(area, proxy)) {
			numVotes += getNumVotes(area, delegation.getFromUser());
			if (numVotes > Integer.MAX_VALUE-1000)
				throw new RuntimeException("There seems to be a circular deleagtion from "+proxy);
		}
		return numVotes;
	}

}
