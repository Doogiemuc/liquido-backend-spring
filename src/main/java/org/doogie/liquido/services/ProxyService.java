package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
	There are two data models for proxy assignments that we need to keep in sync here.

		1) The DelegationModel tracks the delegation from a voter to a proxy in one area.
			 This is used when showing the proxy map of a user. When a voter wants to see who is his direct, transitive top or effective proxy.


		2) The ChecksumModel tracks the anonymous delegation of one checksum to another proxy checksum. No usernames are involved at all!
			 This is used when the proxy casts his vote anonymously. The he casts votes for all checksums that are delegated to his one.
			 The delegation of the checksum can only be set when the proxies checksum is known.
				 a) Either the proxy is a public proxy, then the delegation can immediately be set.
				 b) Or we store a delegation request of the voter that can then be accepted later by the proxy.
						For example when the proxy fetches his voterToken then we know his checksum and can check if there are any requests pointing to it.
 						The delegation request is stored as a DelegationModel
 */
@Slf4j
@Service
public class ProxyService {

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	UserRepo userRepo;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	BallotRepo ballotRepo;

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
	 * @param userVoterToken user's voterToken, so that we calculate his checksum.
	 * @param transitive can the delegation in turn be delegated again by the proxy to another parent proxy.
	 * @return checksumModel of the voter that is either already delegatedTo the proxies checksum or the delegations to the proxy is requested
	 * @throws LiquidoException when the assignment would builder a circular proxy chain or when voterToken is invalid
	 */
	@Transactional
	public ChecksumModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, String userVoterToken, boolean transitive) throws LiquidoException {
		log.info("assignProxy(" + area + ", fromUser=" + fromUser + ", toProxy=" + proxy + " transitive=" + transitive + ")");
		ChecksumModel voterChecksumModel = castVoteService.isVoterTokenValidAndGetChecksum(userVoterToken);
		return assignProxy(area, fromUser, proxy, voterChecksumModel, transitive);
	}

	private ChecksumModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, ChecksumModel voterChecksumModel, boolean transitive) throws LiquidoException {
		//----- sanity checks
		if (area == null || fromUser == null || proxy == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Need area, fromUser and toProxy!");
		//----- check for circular delegation with DelegationModels
		if (thisWouldBeCircularDelegation(area, fromUser, proxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");
		//----- validate voterToken and get voters checksumModel, so that we can delegate it to the proxies checksum anonymously.

		//----- get checksumModel of public proxy (may be null!)
		ChecksumModel proxyChecksumModel = checksumRepo.findByAreaAndPublicProxy(area, proxy);

		//----- IF proxy has a public checksum THEN immediately delegate our checksum to proxies checksum
		if (proxyChecksumModel != null) {
			//----- Check for circular delegation in checksums
			if (thisWouldBeCircularDelegation(voterChecksumModel, proxyChecksumModel))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would lead to a circular delegation of the checksum which cannot be allowed.");

			// ----- upsert delegation from user to proxy in that area
			// implementation note: some more interesting views on upsert in JPA: http://dkublik.github.io/persisting-natural-key-entities-with-spring-data-jpa/
			DelegationModel delegation = delegationRepo.findByAreaAndFromUser(area, fromUser)
					.orElse(new DelegationModel(area, fromUser, proxy));
			delegation.setToProxy(proxy);								// this may overwrite any previous assignment
			delegationRepo.save(delegation);						// Spring's #save method will automatically detect wether delegation is new, depending on wether it has an ID field with value

			//----- store delegation of checksum
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(proxyChecksumModel);
			checksumRepo.save(voterChecksumModel);
			log.info("Delegation from "+fromUser.getEmail()+" to "+proxy.getEmail()+" stored successfully: "+voterChecksumModel.getChecksum()+" -> "+proxyChecksumModel.getChecksum());
		}
		else
		{
			//----- ELSE only store a delegation request
			DelegationModel delegationRequest = DelegationModel.buildDelegationRequest(area, fromUser, proxy, transitive, voterChecksumModel);
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(null);  // MUST delete existing delegation if any
			log.info("Delegation from "+fromUser.getEmail()+" to "+proxy.getEmail()+" requested.");
		}
		return voterChecksumModel;

	}

	/**
	 * Proxy delegations must not be circular. A user must not delegate to a proxy, which already delegated his right
	 * to vote to himself. The proxy delegations must form a tree.
	 * @param voterChecksum token checksum of a voter
	 * @param proxyToCheck token checksum of the new proxy that the user want's to assign
	 * @return true if proxyToCheck is not yet contained in the delegation tree below user.
	 *         false if proxyToCheck already (maybe transitively) delegated his vote to user.
	 */
	public boolean thisWouldBeCircularDelegation(ChecksumModel voterChecksum, ChecksumModel proxyToCheck) {
		//voterChecksum.getDelegatedTo is not yet set and still <null> !
		if (voterChecksum == null) return false;
		if (proxyToCheck == null) return false;
		if (proxyToCheck.getDelegatedTo() == null) return false;
		if (proxyToCheck.getDelegatedTo().equals(voterChecksum)) return true;
		return thisWouldBeCircularDelegation(voterChecksum, proxyToCheck.getDelegatedTo());
	}

	public boolean thisWouldBeCircularDelegation(AreaModel area, UserModel user, UserModel proxyToCheck) {
		if (user.equals(proxyToCheck)) return true;
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
	public ChecksumModel becomePublicProxy(UserModel publicProxy, AreaModel area, String proxyVoterToken) throws LiquidoException {
		log.trace("ENTER becomePublicProxy("+publicProxy+", "+area+")");
		ChecksumModel publicProxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(proxyVoterToken);
		if (publicProxyChecksum == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot become public proxy. Voter token seems to be invalid!");
		publicProxyChecksum.setPublicProxy(publicProxy);
		log.trace("becomePublicProxy: "+publicProxy+ " is now a public proxy");
		return checksumRepo.save(publicProxyChecksum);
	}

	/**
	 * When a proxy accepts all the delegation requests to him,
	 * then we
	 * @param area
	 * @param proxy
	 * @throws LiquidoException
	 */
	@Transactional
	public void acceptDelegationRequests(AreaModel area, UserModel proxy) throws LiquidoException {
		List<DelegationModel> delegationRequests = delegationRepo.findByAreaAndRequestedDelegationTo(area, proxy);
		for(DelegationModel delegation: delegationRequests) {

			this.assignProxy(area, delegation.getFromUser(), proxy, delegation.getRequestedDelegationFromChecksum(), delegation.isTransitive());
			delegation.setRequestedDelegationFromChecksum(null);
			delegation.setRequestedDelegationTo(null);
			delegation.setRequestedDelegationAt(null);
		}
	}

	/**
	 * Get all direct proxies that a voter has delegated his vote to. And also return the top proxy at the end of the transitive delegation chain
	 * if the delegation is transitive.
	 * @return a map with one entry per area where a direct proxy is assigned
	 */
	public Map getDirectProxies(UserModel voter) throws LiquidoException {
		log.debug("ENTER: getDirectProxies(voter="+voter+")");

		/*

   		//
			//   THIS IS NOT NICE      Without delegationModel its getting hard to collect all assigned proxies of a user.
			//

		Lson result = Lson.builder();
		for (AreaModel area: areaRepo.findAll()) {
			ChecksumModel checksumModel = castVoteService.getExistingChecksum(voter, voter.getPasswordHash(), area);
			Lson proxyInArea = Lson.builder()
				.put("directProxy", checksumModel.getDelegatedTo() != null ? checksumModel.getDelegatedTo().getChecksum() : "")
				.put("topProxy",    checksumModel.getDelegatedTo() != null ? findTopChecksum(checksumModel).getChecksum() : "");
			result.put(area.getTitle(), proxyInArea);
		}
		return result;
    */

		return delegationRepo.findByFromUser(voter).stream().collect(Collectors.toMap(delegation -> delegation.getArea(), delegation -> delegation.getToProxy()));

		/* This one liner was formerly known as this ... beginning to like Java8 streams :-)
		List<DelegationModel> delegations = delegationRepo.findByFromUser(fromUser);
		Map<AreaModel, UserModel> proxyMap = new HashMap<>();
		for (DelegationModel delegation : delegations) {
			proxyMap.put(delegation.getArea(), delegation.getToProxy());
		}
		return proxyMap;
    */
	}

	/**
	 * When a user wants to check how his direct proxy has  voted for him.
	 * @param poll a poll
	 * @param voterChecksum the voter's checksum
	 * @return the ballot of the vote's direct proxy in this poll.
	 * @throws LiquidoException when this voter did not delegate his checksum to any proxy in this area
	 */
	public BallotModel getBallotOfDirectProxy(PollModel poll, ChecksumModel voterChecksum) throws LiquidoException {
		if (voterChecksum.getDelegatedTo() == null)
			throw new LiquidoException(LiquidoException.Errors.NO_DELEGATION, "You did not delegate your vote");
		BallotModel proxyBallot = ballotRepo.findByPollAndChecksum(poll, voterChecksum.getDelegatedTo());
		return proxyBallot;
	}

	public BallotModel getBallotOfTopProxy(PollModel poll, ChecksumModel voterChecksum) throws LiquidoException {
		if (voterChecksum.getDelegatedTo() == null)
			throw new LiquidoException(LiquidoException.Errors.NO_DELEGATION, "You did not delegate your vote");
		ChecksumModel topChecksum = findTopChecksum(voterChecksum);
		BallotModel proxyBallot = ballotRepo.findByPollAndChecksum(poll, topChecksum);
		return proxyBallot;
	}

	ChecksumModel findTopChecksum(ChecksumModel checksum) {
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
		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, voter);
		if (!delegation.isPresent()) return voter;
		return findTopProxy(area, delegation.get().getToProxy());
	}



	/**
	 * Delete the delegation from this user to his proxy in this area. Will also remove the delegateTo
	 * from the user's ChecksumModel.
	 * @param area the area of the delegation
	 * @param fromUser the user that delegated his right to vote to a proxy
	 * @param voterToken valid token of fromUser. We need this to remove the delegateTo from the anonymous ChecksumModel
	 * @throws LiquidoException
	 */
	@Transactional
	public void removeProxy(AreaModel area, UserModel fromUser, String voterToken) throws LiquidoException {
		log.info("removeProxy("+area+", fromUser="+fromUser+")");
		ChecksumModel checksumModel = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);

		//----- remove the delegation of the voter's checksum
		if (checksumModel != null) {
			checksumModel.setDelegatedTo(null);
			checksumRepo.save(checksumModel);
		}

		//----- delete the DelegationModel
		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		if (delegation.isPresent()) {
			delegationRepo.delete(delegation.get());
		}

	}


	/**
	 * Get the number of votes a proxy may cast because of (transitive) delegations to him. Including his own vote.
	 * @param voterToken the voterToken of a voter who might be a proxy.
	 * @return the number of votes this proxy may cast, including his own one.
	 */
	public long getNumVotes(String voterToken) throws LiquidoException {
		ChecksumModel proxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		return countNumVotesRecursive(proxyChecksum, true, 0);
	}

	/**
	 * Recursively count the number of votes a proxy may cast, including his own vote.
	 * This recursive method is meant to be called with includeNonTransitiveDelegations = true and depth = 0, because non transitive delegations
	 * do count on depth == 0. They are direct delegations. On deeper levels, only transitive delegations are added up.
	 *
	 * @param checksum a checksum that may have delegations to it
	 * @param includeNonTransitiveDelegations weather to include non transitive delegations in the count.
	 * @param depth current recursion depth
	 * @return the number of delegations to this proxy (without the proxy himself)
	 */
	public long countNumVotesRecursive(ChecksumModel checksum, boolean includeNonTransitiveDelegations, long depth) {
		if (checksum == null) return 0;
		if (depth > Long.MAX_VALUE - 10) throw new RuntimeException("There seems to be a circular delegation with checksum: "+checksum.getChecksum());
		long numVotes = 1;
		List<ChecksumModel> delegations = checksumRepo.findByDelegatedToAndTransitive(checksum, true);
		if (includeNonTransitiveDelegations) {
			delegations.addAll(checksumRepo.findByDelegatedToAndTransitive(checksum, false));
		}
		for (ChecksumModel delegatedChecksum: delegations) {
			numVotes += countNumVotesRecursive(delegatedChecksum, false, depth+1);
		}
		return numVotes;
	}

}
