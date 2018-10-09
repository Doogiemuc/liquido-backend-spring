package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	public Map<AreaModel, UserModel> getProxyMap(UserModel fromUser) {
		return delegationRepo.findByFromUser(fromUser).stream().collect(Collectors.toMap(delegation -> delegation.getArea(), delegation -> delegation.getToProxy()));
		/* This one lineer was formerly known as this ... beginning to like Java8 streams :-)
		List<DelegationModel> delegations = delegationRepo.findByFromUser(fromUser);
		Map<AreaModel, UserModel> proxyMap = new HashMap<>();
		for (DelegationModel delegation : delegations) {
			proxyMap.put(delegation.getArea(), delegation.getToProxy());
		}
		return proxyMap;
		*/
	}


	// used for testing. normally proxy should be assigned with a valid voterToken
	public DelegationModel assignProxyWithPassword(AreaModel area, UserModel fromUser, UserModel toProxy, String fromUserPasswordHash) throws LiquidoException {
		String voterToken		 = castVoteService.getVoterToken(fromUser, area);
		return this.assignProxy(area, fromUser, toProxy, voterToken);
	}

	/**
	 * User forwards his right to vote to a proxy in one area.
	 * @param area area in which to assign the proxy
	 * @param fromUser voter that forwards his right to vote
	 * @param toProxy proxy that receives the right and thus can vote in place of fromUser
	 * @param voterToken user's voterToken that MUST already have a valid checksum stored at the server
	 * @return the newly created or updated DelegationModel or null, when the proxy is not public and must still confirm the delegation
	 * @throws LiquidoException when the assignment would create a circular proxy chain, when voterToken is invalid
	 */
	public DelegationModel assignProxy(AreaModel area, UserModel fromUser, UserModel toProxy, String voterToken) throws LiquidoException {
		log.trace("assingProxy("+area+", fromUser="+fromUser+", toProxy="+toProxy+")");
		//----- sanity checks
		if (!castVoteService.isVoterTokenValid(voterToken))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Passed VoterToken is invalid!");

		//----- check for circular delegation, which cannot allowed
		if (thisWouldBeCircularDelegation(area, fromUser, toProxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");

		//----- (1) upsert delegation from user to proxy in that area
		DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		DelegationModel savedDelegation;
		if (existingDelegation == null) {
			DelegationModel delegation = new DelegationModel(area, fromUser, toProxy);
			savedDelegation = delegationRepo.save(delegation);
		} else {
			existingDelegation.setToProxy(toProxy);
			savedDelegation = delegationRepo.save(existingDelegation);
		}
		// implementation note: some more interesting views un upsert in JPA: http://dkublik.github.io/persisting-natural-key-entities-with-spring-data-jpa/

		//----- (2) Delegate users checksum to the proxies checksum
		String voterChecksum = castVoteService.calcChecksumFromVoterToken(voterToken);
		TokenChecksumModel voterChecksumModel = checksumRepo.findByChecksum(voterChecksum);
		if (voterChecksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: voterToken seems to be invalid. Cannot not find its checksum.");

		//----- (3) IF proxy has a public checksum THEN delegate to it ELSE ass a task, that proxy must confirm the delegation
		TokenChecksumModel proxyChecksumModel = this.getChecksumModelOfPublicProxy(area, toProxy);
		if (proxyChecksumModel == null) {
			// store a task, that proxy must confirm the delegation.  (fromUser, toProxy, voterChecksumModel)
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


	public TokenChecksumModel becomePublicProxy(UserModel publicProxy, AreaModel area) throws LiquidoException {
		log.trace("becomePublicProxy("+publicProxy+", "+area+")");
		String voterToken		 = castVoteService.getVoterToken(publicProxy, area);
		String proxyChecksum = castVoteService.calcChecksumFromVoterToken(voterToken);
		TokenChecksumModel publicProxyChecksum = new TokenChecksumModel(proxyChecksum, area);
		publicProxyChecksum.setPublicProxy(publicProxy);
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
	 */
	public TokenChecksumModel getChecksumModelOfPublicProxy(AreaModel area, UserModel proxy) {
		return checksumRepo.findByAreaAndPublicProxy(area, proxy);
	}

	/**
	 * When a user wants to know how his proxy voted.
	 * @param poll a poll at least in voting phase
	 * @param voter a voter with a (transitive) proxy
	 * @return The ballot of the top most proxy so that the user can see how his delegation was used in the end.
	 *         Or null if voter has no proxy assigned in the poll's area.
	 */
	public BallotModel getBallotOfTopProxy(PollModel poll, UserModel voter) {
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(poll.getArea(), voter);
		if (delegation == null) return null;
		UserModel topProxy = findTopmostProxy(delegation);
		TokenChecksumModel topProxyChecksumModel = getChecksumModelOfPublicProxy(poll.getArea(), topProxy);
		return ballotRepo.findByPollAndChecksum(poll, topProxyChecksumModel.getChecksum());
	}

	/**
	 * Recursively find the "transitive" proxy at the top of the tree trunk starting at the given delegation.
	 * @param delegation a delegation from a user to a proxy. Must not be null!
	 * @return the proxy at the top of the delegation chain
	 */
	public UserModel findTopmostProxy(DelegationModel delegation) {
		DelegationModel transitiveDelegeation = delegationRepo.findByAreaAndFromUser(delegation.getArea(), delegation.getToProxy());
		if (transitiveDelegeation == null) return delegation.getToProxy();
		return findTopmostProxy(transitiveDelegeation);
	}


	//TODO: removeProxy(UserModel, fromUser, AreaModel area)

}
