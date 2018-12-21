package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

	@Autowired
	EntityLinks entityLinks;

	/**
	 * User forwards his right to vote to a proxy in one area.
	 *
	 * After some sanity checks, this methods checks for a circular delegation which would be forbidden.
	 * If toProxy is a public proxy, then we can directly delegate user's checksum to the public proxie's checksum.
	 * Otherwise we store a delegation request, so that the proxy needs to accept the delegation.
	 *
	 * The checksum of the voter that wants to delegate his right to vote to the proxy is stored in the delegation request.
	 * When the proxy accepts the delegation, then the voter's checksum.delegatedTo can be set to the proxies checksum.
	 *
	 * The DelegationModel of the delegation request already has `toProxy` set. You must consider {@link DelegationModel#isDelegationRequest()}
	 * when looking at the tree of delegations. The ChecksumModel only have their delegatedTo set, after the delegation
	 * is accepted by the proxy.
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
		ChecksumModel voterChecksum = castVoteService.isVoterTokenValidAndGetChecksum(userVoterToken);
		castVoteService.refreshChecksum(voterChecksum);
		Optional<ChecksumModel> proxyChecksum = getChecksumOfPublicProxy(area, proxy);   // get checksumModel of public proxy (may be null!)
		return assignProxy(area, fromUser, proxy, voterChecksum, proxyChecksum, transitive);
	}

	/**
	 * This part of assigning a proxy was refactored into its own (private) method, because
	 * when a proxy accepts pending delegations, we already have the checksum of the delegee, but not his voter token which is confidential.
	 * @return the voter's checksum that is then eiter delegated or requested the delegation to proxy
	 */
	@Transactional
	private ChecksumModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, ChecksumModel voterChecksumModel, Optional<ChecksumModel> proxyChecksum, boolean transitive) throws LiquidoException {
		log.info("assignProxy(" + area + ", fromUser=" + fromUser + ", toProxy=" + proxy + " transitive=" + transitive + ")");
		//----- sanity checks
		if (area == null || fromUser == null || proxy == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Need area, fromUser and toProxy!");
		//----- check for circular delegation with DelegationModels
		if (thisWouldBeCircularDelegation(area, fromUser, proxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");
		//----- validate voterToken and get voters checksumModel, so that we can delegate it to the proxies checksum anonymously.

		// ----- upsert delegation from user to proxy in that area
		// implementation note: some more interesting views on upsert in JPA: http://dkublik.github.io/persisting-natural-key-entities-with-spring-data-jpa/
		Optional<DelegationModel> delegationOpt = delegationRepo.findByAreaAndFromUser(area, fromUser);
		DelegationModel delegation;
		if (delegationOpt.isPresent()) {
			delegation = delegationOpt.get();
			log.trace("assignProxy: Updating existing delegation (id="+delegation.getId());
		} else {
			log.trace("assignProxy: Create new delegation");
			delegation = new DelegationModel(area, fromUser, proxy);
		}

		//----- IF proxy has a public checksum THEN immediately delegate our checksum to proxies checksum
		if (proxyChecksum.isPresent()) {
			//----- Check for circular delegation in tree of checksums
			if (thisWouldBeCircularDelegation(voterChecksumModel, proxyChecksum.get()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would lead to a circular delegation of the checksum which cannot be allowed.");

			delegation.setToProxy(proxy);								// this may overwrite any previous assignment
			delegation.setRequestedDelegationFromChecksum(null);
			delegation.setRequestedDelegationAt(null);
			delegation.setTransitive(transitive);
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(proxyChecksum.get());
			log.info("assignProxy: Proxy assigned "+delegation);
		}
		else
		{
			//----- ELSE only store a delegation request AND do not store any delegation at the checksum
			// Requested DelegationModels already contain their toProxy
			// But the ChecksumModel has delegatedTo set to null
			//
			delegation.setToProxy(proxy);										// a requested delegation already contains the toProxy!
			delegation.setRequestedDelegationFromChecksum(voterChecksumModel);
			delegation.setRequestedDelegationAt(LocalDateTime.now());
			delegation.setTransitive(transitive);
			voterChecksumModel.setTransitive(transitive);
			voterChecksumModel.setDelegatedTo(null);  			// MUST delete existing delegation if any
			log.info("assignProxy: Delegation requested "+delegation);
		}

		//----- save delegation and checksum
		delegationRepo.save(delegation);						// Spring's #save method will automatically detect if delegation is new, depending on its ID field.
		checksumRepo.save(voterChecksumModel);
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
		//TODO: What about delegation requests?
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
		publicProxyChecksum.setPublicProxy(publicProxy);
		checksumRepo.save(publicProxyChecksum);  // MUST save before I can accept delegation requests
		acceptDelegationRequests(area, publicProxy, proxyVoterToken);
		log.info("becomePublicProxy: "+publicProxy+ " is now a public proxy");
		return checksumRepo.save(publicProxyChecksum);
	}

	//TODO: delete (public) proxy: remove all delegations

	/**
	 * Get the checksum of a public proxy, so that a voter can delegate to it.
	 * This can also be used to check if a user alreayd is a public proxy.
	 * @param area Area
	 * @param proxy proxy to check
	 * @return (optionally) the checksum of a public proxy
	 */
	public Optional<ChecksumModel> getChecksumOfPublicProxy(AreaModel area, UserModel proxy) {
		return checksumRepo.findByAreaAndPublicProxy(area, proxy);
	}

	public List<DelegationModel> findAcceptedDelegations(AreaModel area, UserModel proxy) {
		return delegationRepo.findAcceptedDelegations(area, proxy);
	}

	public List<DelegationModel> findDelegationRequests(AreaModel area, UserModel proxy) {
		return delegationRepo.findDelegationRequests(area, proxy);
	}

	/**
	 * GIVEN a proxy that has pending delegation requests
	 *  WHEN this proxy accepts all the delegation requests to him,
	 *  THEN all delegations are assigned (DelegationModel and Checksum delegation)
	 *   AND the requests are cleared.
	 * @param area area for delegation
	 * @param proxy the proxy that accepts all pending delegation requests to him
	 * @return the newly counted number of votes that his proxy now may cast.
	 * @throws LiquidoException when one of the delegations cannot be assigned
	 */
	@Transactional
	public long acceptDelegationRequests(AreaModel area, UserModel proxy, String proxyVoterToken) throws LiquidoException {
		log.debug("=> accept delegation requests for proxy "+proxy+" in area.id="+area.getId());
		List<DelegationModel> delegationRequests = delegationRepo.findDelegationRequests(area, proxy);
		Optional<ChecksumModel> proxyChecksum = Optional.of(castVoteService.isVoterTokenValidAndGetChecksum(proxyVoterToken));
		for(DelegationModel delegation: delegationRequests) {
			log.trace("Accepting delegation request from "+delegation.getFromUser()+" to proxy "+delegation.getToProxy());
			this.assignProxy(area, delegation.getFromUser(), proxy,	delegation.getRequestedDelegationFromChecksum(), proxyChecksum, delegation.isTransitive());
		}
		long delegationCount = getDelegationCount(proxyVoterToken);
		log.info("<= accepted "+delegationRequests.size()+" delegation requests for proxy "+proxy+" in area.id="+area.getId()+ ", new delegationCount="+delegationCount);
		return delegationCount;
	}

	/**
	 * Collect all the proxy information for voter in that area.
	 * Delegation to direct proxy,
	 * The topmost proxy in the delegation chain starting at voter
	 * If voter already is a public proxy
	 * Pending delegation requests
	 * Number of votes this user already has because of delegations (including his own vote)
	 *
	 * @param voter who's info to fetch
	 * @param voterToken need user's voterToken to get numVotes
	 * @return <pre>
	 *   {
	 *     directProxyDelegation: { ... },
	 *     topProxy: { ... },
	 *     isPublicProxy: { ... },
	 *     delegationRequests: { ... },
	 *     numVotes: 42
 	 *   }
	 * </pre>
	 */
	public Lson getProxyInfo(AreaModel area, UserModel voter, String voterToken) throws LiquidoException {
		//TODO: Is a service method allowed to return schemaless Lson. Or should it return a DTO? (overkill!)   The REST resource must return JSON anyway.
		log.debug("ENTER: getProxyMap("+voter+")");

		Link areaLink = entityLinks.linkToSingleResource(area);
		Optional<DelegationModel> directProxy = delegationRepo.findByAreaAndFromUser(area, voter);
		Optional<UserModel> topProxy = findTopProxy(area, voter);
		Optional<ChecksumModel> checksumOfPublicProxy = getChecksumOfPublicProxy(area, voter);

		// manually build a JSON similar to the HATEOAS structure under _links
		Lson proxyInfo = Lson.builder()
			.put("area", area)				// also directly inline the full area JSON, because client needs it
			.put("directProxyDelegation", directProxy.orElse(null))			// may also be a requested delegation
			.put("topProxy", topProxy.orElse(null))   // this inlines the topProxy information, because client needs it
			.put("isPublicProxy", checksumOfPublicProxy.isPresent())
			.put("delegationCount", getDelegationCount(voterToken))
			.put("delegationRequests", findDelegationRequests(area, voter))
			.put("_links.area.href", areaLink.getHref())
			.put("_links.area.templated", areaLink.isTemplated());
  		//TODO: .put("_links.topProxy.href", topProxyLink.getHref())

		return proxyInfo;

	}

	/**
	 * Get the directly assigned proxy of this voter.
	 * If there only is a delegation request this method will return Optional.empty()
	 * @param area an area
	 * @param voter a voter that may have a proxy assigned in area
	 * @return the direct proxy of this voter if he has an accepted delegation to a proxy in this area
	 */
	public Optional<UserModel> getDirectProxy(AreaModel area, UserModel voter) {
		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, voter);
		if (delegation.isPresent() && !delegation.get().isDelegationRequest()) return Optional.of(delegation.get().getToProxy());
		return Optional.empty();
	}

	/**
	 * Recursively find the "transitive" top proxy at the top of the delegation chain for this voter.
	 * This only considers accepted delegations not requested delegations.
	 * Keep in mind that the topProxy is not necessarily the user that voted for the delegee in a specific poll.
	 * It may be any proxy between voter and the topProxy in the chain of delegations.
	 *
	 * @param area the area of the delegation
	 * @param voter a voter that may have delegated his right to vote to a proxy
	 * @return (optionally) the proxy at the top of the delegation chain starting at voter
	 *         or Optional.empty() if voter has no proxy in that area.
	 */
	public Optional<UserModel> findTopProxy(AreaModel area, UserModel voter) {
		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, voter);
		if (delegation.isPresent() && !delegation.get().isDelegationRequest()) {
			@NotNull UserModel directProxy = delegation.get().getToProxy();
			if (delegation.get().isTransitive()) {
				Optional<UserModel> topProxy = findTopProxy(area, directProxy);
				return topProxy.isPresent() ? topProxy : Optional.of(directProxy);
			} else {
				return Optional.of(directProxy);
			}
		} else {
			return Optional.empty();
		}
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
	 * Get the number of accepted delegations that a proxy received.
	 * The proxy may vote that many times plus his own vote.
	 * @param voterToken the voterToken of a voter who might be a proxy.
	 * @return the number of delegations that this proxy has.
	 */
	public long getDelegationCount(String voterToken) throws LiquidoException {
		ChecksumModel proxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		return countDelegationsRec(proxyChecksum, true, 0);
	}

	/**
	 * Recursively count the number accepted delegations that a proxy has received.
	 * This recursive method is meant to be called with includeNonTransitiveDelegations = true and depth = 0, because non transitive delegations
	 * do count on depth == 0. They are direct delegations. On deeper levels, only transitive delegations are added up.
	 *
	 * @param checksum a checksum that may have delegations to it
	 * @param includeNonTransitiveDelegations weather to include non transitive delegations in the count.
	 * @param depth current recursion depth
	 * @return the number of delegations to this proxy (without the proxies own vote)
	 */
	private long countDelegationsRec(ChecksumModel checksum, boolean includeNonTransitiveDelegations, long depth) {
		if (checksum == null) return 0;
		if (depth > Long.MAX_VALUE - 10) throw new RuntimeException("There seems to be a circular delegation with checksum: "+checksum.getChecksum());
		long numVotes = 0;
		List<ChecksumModel> delegations = checksumRepo.findByDelegatedToAndTransitive(checksum, true);
		if (includeNonTransitiveDelegations) {
			delegations.addAll(checksumRepo.findByDelegatedToAndTransitive(checksum, false));
		}
		for (ChecksumModel delegatedChecksum: delegations) {
			numVotes += 1 + countDelegationsRec(delegatedChecksum, false, depth+1);
		}
		return numVotes;
	}

}
