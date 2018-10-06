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
		//TODO: check that voterToken is valid and that it's checksum is known

		//----- check for circular dependency, which is not allowed
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
		if (voterChecksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assignProxy: voterToken seems to be invalid. Cannot not find its checksum.");

		//----- (3) IF proxy has a public checksum THEN delegate to it ELSE ass a task, that proxy must confirm the delegation
		TokenChecksumModel proxyChecksumModel = this.getChecksumModelOfPublicProxy(toProxy);
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

	public TokenChecksumModel becomePublicProxy(UserModel publicProxy, AreaModel area) throws LiquidoException {
		log.trace("becomePublicProxy("+publicProxy+", "+area+")");
		String voterToken		 = castVoteService.getVoterToken(publicProxy, area);
		String proxyChecksum = castVoteService.getChecksumFromVoterToken(voterToken);
		TokenChecksumModel publicProxyChecksum = new TokenChecksumModel(proxyChecksum);
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
	public TokenChecksumModel getChecksumModelOfPublicProxy(UserModel proxy) {
		return checksumRepo.findByPublicProxy(proxy);
	}

	//TODO: removeProxy(UserModel, fromUser, AreaModel area)

}
