package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
	 * Get all direct proxies that a voter has delegated his vote to.
	 * @param fromUser the voter
	 * @return a map with one entry per area where a direct proxy is assigned
	 */
	public Map<AreaModel, UserModel> getDirectProxies(UserModel fromUser) {
		log.debug("getDirectProxies(fromUser="+fromUser+")");
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
		String voterToken		 = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area);
		return this.assignProxy(area, fromUser, toProxy, voterToken);
	}
	*/

	/**
	 * User forwards his right to vote to a proxy in one area.
	 *
	 * After some sanity checks, this methods checks for a circular delegation which would be forbidden.
	 * If toProxy is a public proxy, then we can directly delegate user's checksum to the public proxie's checksum.
	 * Otherwise we store a delegation request, so that the proxy needs to accept the delegation.
	 *
	 * @param area area in which to assign the proxy
	 * @param fromUser voter that forwards his right to vote
	 * @param proxy proxy that receives the right and thus can vote in place of fromUser
	 * @param voterToken user's voterToken, so that we calculate his checksum. BE CAREFULL, DO NOT PASS a password
	 * @param transitive can the delegation in turn be delegated again by the proxy to another parent proxy.
	 * @return checksumModel of the voter that is either already delegatedTo the proxies checksum or the delegations to the proxy is requested
	 * @throws LiquidoException when the assignment would builder a circular proxy chain or when voterToken is invalid
	 */
	@Transactional
	public TokenChecksumModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, String voterToken, boolean transitive) throws LiquidoException {
		log.trace("ENTER assignProxy("+area+", fromUser="+fromUser+", toProxy="+proxy+" transitive="+transitive+")");
		//----- sanity checks
		if (area == null || fromUser == null || proxy == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Need area, fromUser and toProxy!");
		if (fromUser.equals(proxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Must not delegate to yourself!");
		//----- check for circular delegation, which cannot be allowed
		if (thisWouldBeCircularDelegation(area, fromUser, proxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");

		//----- validate voterToken and get voters checksumModel, so that we can delegate it to the proxies checksum anonymously.
		TokenChecksumModel voterChecksumModel;
		try {
			voterChecksumModel = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		} catch (LiquidoException e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: voterToken is invalid.", e);
		}

		/* ----- upsert delegation from user to proxy in that area
		DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		DelegationModel savedDelegation;
		if (existingDelegation == null) {
			DelegationModel delegation = new DelegationModel(area, fromUser, proxy);
			savedDelegation = delegationRepo.save(delegation);
		} else {
			existingDelegation.setToProxy(proxy);													// this may overwrite any previous assignment
			savedDelegation = delegationRepo.save(existingDelegation);
		}
		// implementation note: some more interesting views un upsert in JPA: http://dkublik.github.io/persisting-natural-key-entities-with-spring-data-jpa/
		*/

		TokenChecksumModel proxyChecksumModel = checksumRepo.findByAreaAndPublicProxy(area, proxy);
		//----- IF proxy has a public checksum THEN delegate to it
		if (proxyChecksumModel != null) {
			if (voterChecksumModel.equals(proxyChecksumModel))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Must not delegate checksum to itself!");
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(proxyChecksumModel);
			voterChecksumModel.setRequestedDelegationTo(null);    // MUST delete existing request, if any
			voterChecksumModel.setRequestedDelegationAt(null);
			checksumRepo.save(voterChecksumModel);
			log.info("Delegation from "+fromUser.getEmail()+" to "+proxy.getEmail()+" stored successfully: "+voterChecksumModel.getChecksum()+" -> "+proxyChecksumModel.getChecksum());
		} else {
			// ELSE add a delegation request
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(null);  // MUST delete existing delegation if any
			voterChecksumModel.setRequestedDelegationTo(proxy);
			voterChecksumModel.setRequestedDelegationAt(LocalDateTime.now());
			log.info("Delegation from "+fromUser.getEmail()+" to "+proxy.getEmail()+" requested.");
		}
		return voterChecksumModel;

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


	/**
	 * When a user opts-in to become a public proxy, then his username is stored to together with his checksum,
	 * so that other users can delegate their right to vote to this "public" checksum.
	 * @param publicProxy the voter that wants to become a public proxy
	 * @param area voter is a proxy in an area
	 * @param proxyVoterToken the voters token
	 * @return the proxy's public checksum
	 * @throws LiquidoException when voterToken is invalid
	*/
	@Transactional
	public TokenChecksumModel becomePublicProxy(UserModel publicProxy, AreaModel area, String proxyVoterToken) throws LiquidoException {
		log.trace("ENTER becomePublicProxy("+publicProxy+", "+area+")");
		TokenChecksumModel publicProxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(proxyVoterToken);
		if (publicProxyChecksum == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot become public proxy. Voter token seems to be invalid!");
		publicProxyChecksum.setPublicProxy(publicProxy);
		log.trace("becomePublicProxy: "+publicProxy+ " is now a public proxy");
		return checksumRepo.save(publicProxyChecksum);
	}


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
		log.debug("ENTER removeProxy("+area+", fromUser="+fromUser+")");
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		TokenChecksumModel checksumModel = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		if (delegation != null) delegationRepo.delete(delegation);
		if (checksumModel != null) {
			checksumModel.setDelegatedTo(null);
			checksumRepo.save(checksumModel);
		}
	}


	/**
	 * Get the number of votes a proxy may cast because of (transitive) delegations to him. Including his own vote.
	 * @param voterToken the voterToken of a voter who might be a proxy.
	 * @return the number of votes this proxy may cast, including his own one.
	 */
	public long getNumVotes(String voterToken) throws LiquidoException {
		TokenChecksumModel proxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		return countNumVotesRecursive(proxyChecksum, true, 0);
	}

	/**
	 * Recursively count the number of votes a proxy may cast, including his own vote.
	 * This recursive method is meant to be called with includeNonTransitiveDelegations = true and depth = 0, because non transitive delegations
	 * do count on depth == 0. They are direct delegations. On deeper levels, only transitive delegations are added up.
	 *
	 * @param checksum a checksum that may have delegations to it
	 * @param includeNonTransitiveDelegations wether to include non transitive delegations in the count.
	 * @param depth current recursion depth
	 * @return the number of delegations to this proxy (without the proxy himself)
	 */
	public long countNumVotesRecursive(TokenChecksumModel checksum, boolean includeNonTransitiveDelegations, long depth) {
		if (checksum == null) return 0;
		if (depth > Long.MAX_VALUE - 10) throw new RuntimeException("There seems to be a circular delegation with checksum: "+checksum.getChecksum());
		long numVotes = 1;
		List<TokenChecksumModel> delegations = checksumRepo.findByDelegatedToAndTransitive(checksum, true);
		if (includeNonTransitiveDelegations) {
			delegations.addAll(checksumRepo.findByDelegatedToAndTransitive(checksum, false));
		}
		for (TokenChecksumModel delegatedChecksum: delegations) {
			numVotes += countNumVotesRecursive(delegatedChecksum, false, depth+1);
		}
		return numVotes;
	}

}
