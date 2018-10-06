package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	// used for testing. normally proxy should be assigned with a valid voterToken
	public DelegationModel assignProxyWithPassword(AreaModel area, UserModel fromUser, UserModel toProxy, String fromUserPasswordHash) throws LiquidoException {
		String voterToken		 = castVoteService.upsertVoterTokenAndChecksum(fromUser.getId(), fromUserPasswordHash, area.getId());
		return this.assignProxy(area, fromUser, toProxy, voterToken);
	}

	/**
	 * User forwards his right to vote to a proxy in one area.
	 * @param area area in which to assign the proxy
	 * @param fromUser voter that forwards his right to vote
	 * @param toProxy proxy that receives the right and thus can vote in place of fromUser
	 * @param voterToken user's voterToken that MUST already have a valid checksum stored at the server
	 * @return the newly created or updated DelegationModel
	 * @throws LiquidoException when the assignment would create a circular proxy chain, when voterToken is invalid
	 */
	public DelegationModel assignProxy(AreaModel area, UserModel fromUser, UserModel toProxy, String voterToken) throws LiquidoException {
		log.trace("assingProxy("+area+", fromUser="+fromUser+", toProxy="+toProxy+")");
		//----- (0) check for circular dependency, which is not allowed
		//TODO:  recursively check delegees if toProxy is in there => error

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
		String voterChecksum = castVoteService.getChecksumFromVoterToken(voterToken);
		TokenChecksumModel voterChecksumModel = checksumRepo.findByChecksum(voterChecksum);
		if (voterChecksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: Could not find checksum of voter");  // This should never happen, because we just created and stored this checksum

		//----- (3) IF proxy has a public checksum THEN delegate to it ELSE ass a task, that proxy must confirm the delegation
		/*
		TokenChecksumModel proxyChecksumModel = proxyService.getChecksumModelOfPublicProxy(toProxy);
		if (proxyChecksumModel == null) {
			// store a task, that proxy must confirm the delegation.  (fromUser, toProxy, voterChecksumModel)
		}

		*/

		//===================JUTS FOR TESTING: MAKE SURE THAT PROXY HAS A CHECKSUM
		String proxyPasswordHash = "dummyProxyPassword";
		String proxyToken		 = castVoteService.upsertVoterTokenAndChecksum(fromUser.getId(), proxyPasswordHash, area.getId());
		String proxyChecksum = castVoteService.getChecksumFromVoterToken(proxyToken);
		TokenChecksumModel proxyChecksumModel = checksumRepo.findByChecksum(proxyChecksum);
		if (proxyChecksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: Could not find checksum of proxy");

		voterChecksumModel.setDelegatedTo(proxyChecksumModel);
		checksumRepo.save(voterChecksumModel);

		return savedDelegation;
	}

	//TODO: removeProxy(UserModel, fromUser, AreaModel area)

}
