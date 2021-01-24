package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.RightToVoteModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
	RightToVoteRepo rightToVoteRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	EntityLinks entityLinks;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	/**
	 * Get all users that could be assigned as a proxy in this area.
	 * Assignable proxies are all users, except the current user, his already assigned proxy (if any) or
	 * any proxy that would create a circular delegation which is not allowed.
	 * @param area an area
	 * @return the list of assignable proxies
	 * @throws LiquidoException when not logged in
	 */
	public List<UserModel> getAssignableProxies(AreaModel area) throws LiquidoException {
		List<UserModel> assignableProxies = new ArrayList<>();
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your assignable proxies."));
		Optional<DelegationModel> currentProxyOpt = delegationRepo.findByAreaAndFromUser(area, currentUser);
		for (UserModel proxy : userRepo.findAll()) {
			if (!(proxy.equals(currentUser) ||
				    currentProxyOpt.isPresent() && currentProxyOpt.get().equals(proxy) ||
				    thisWouldBeCircularDelegation(area, currentUser, proxy)))
			{
				assignableProxies.add(proxy);
			}
		}
		return assignableProxies;
	}

	/**
	 * User forwards his right to vote to a proxy in one area.
	 *
	 * @param area area in which to assign the proxy
	 * @param fromUser voter that forwards his right to vote
	 * @param proxy proxy that receives the right and thus can vote in place of fromUser
	 * @param userVoterToken user's voterToken, so that we calculate his checksum.
	 * @return DelegationModel requested or already accepted delegation
	 * @throws LiquidoException when the assignment would builder a circular proxy chain or when voterToken is invalid
	 */
	public DelegationModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, String userVoterToken) throws LiquidoException {
		//----- validate voterToken and get voters checksumModel, so that we can delegate it to the proxies checksum anonymously.
		//TODO: How to check if the passed voterToken is from the currently logged in user?  (Currently this would be possible. As long as the server can create voterTokens for himself.)
		RightToVoteModel rightToVote = castVoteService.isVoterTokenValid(userVoterToken);
		Optional<RightToVoteModel> rightToVoteOfProxy = getRightToVoteOfPublicProxy(area, proxy);
		return assignProxy(area, fromUser, proxy, rightToVote, rightToVoteOfProxy);
	}

	/**
	 * Forward own right to vote to a proxy in one area
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
	 * This part of assigning a proxy was refactored into its own (private) method, because
	 * when a proxy accepts pending delegations, we already have the checksum of the delegee, but not his voter token which is confidential.
	 *
	 * @return DelegationModel with requested or already accepted delegation
	 */
	@Transactional
	private DelegationModel assignProxy(AreaModel area, UserModel fromUser, UserModel proxy, RightToVoteModel rightToVote, Optional<RightToVoteModel> proxyRightToVote) throws LiquidoException {
		log.info("assignProxy(" + area + ", fromUser=" + fromUser.toStringShort() + ", toProxy=" + proxy.toStringShort() + ")");
		//----- sanity checks
		if (area == null || fromUser == null || proxy == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Need area, fromUser and toProxy!");
		//----- check for circular delegation with DelegationModels
		if (thisWouldBeCircularDelegation(area, fromUser, proxy))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would be a circular delegation which cannot be allowed.");
		//----- if voterChecksum has a public proxy then this must be fromUser
		if (rightToVote.getPublicProxy() != null && !rightToVote.getPublicProxy().equals(fromUser))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot assign proxy. Voter's checksum does not belong to voter!");


		//------ refresh validity of token's checksum
		castVoteService.refreshRightToVote(rightToVote);

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
		if (proxyRightToVote.isPresent()) {
			//----- Check for circular delegation in tree of checksums
			if (thisWouldBeCircularDelegation(rightToVote, proxyRightToVote.get()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY, "Cannot assign proxy. This would lead to a circular delegation of the checksum which cannot be allowed.");

			delegation.setToProxy(proxy);								// this may overwrite any previous assignment
			delegation.setRequestedDelegationFrom(null);
			delegation.setRequestedDelegationAt(null);
			rightToVote.setDelegatedTo(proxyRightToVote.get());

			// Remark:
			// Theoretically there could be some polls that are currently open for voting and fromUser did not vote in them yet, but his new proxy already did.
			// LIQUIDO will not automatically create delegated ballots for these polls here. Only future votes of his proxy will also be counted for fromUser.
			// Of course fromUser may ALWAYS vote for himself in ANY poll.

			log.info("assignProxy: Proxy assigned "+delegation);
		}
		else
		{
			//----- ELSE only store a delegation request AND do not store any delegation at the checksum
			// Requested DelegationModels already have the toProxy set. You must check for isRequested!
			// The ChecksumModel has delegatedTo set to null. This will be set when the request is being accepted.
			//
			delegation.setToProxy(proxy);										// a requested delegation already contains the toProxy!
			delegation.setRequestedDelegationFrom(rightToVote);
			delegation.setRequestedDelegationAt(LocalDateTime.now());
			rightToVote.setDelegatedTo(null);  			// MUST delete existing delegation if any
			log.info("assignProxy: Delegation requested "+delegation);
		}

		//----- save delegation and checksum
		delegationRepo.save(delegation);						// Spring's #save method will automatically detect if delegation is new, depending on its ID field.
		rightToVoteRepo.save(rightToVote);
		return delegation;

	}

	/**
	 * Proxy delegations must not be circular. A user must not delegate to a proxy, which already delegated his right
	 * to vote to himself. The proxy delegations must form a tree.
	 * @param rightToVote rightToVote of a voter
	 * @param rightToVoteOfProxy rightToVote of the new proxy that the user want's to delegate to
	 * @return true if rightToVoteOfProxy is not yet contained in the delegation tree below user.
	 *         false if rightToVoteOfProxy already (maybe transitively) delegated his vote to user.
	 */
	public boolean thisWouldBeCircularDelegation(RightToVoteModel rightToVote, RightToVoteModel rightToVoteOfProxy) {
		//rightToVote.getDelegatedTo is not yet set and still <null> !
		if (rightToVote == null) return false;
		if (rightToVoteOfProxy == null) return false;
		if (rightToVoteOfProxy.getDelegatedTo() == null) return false;
		if (rightToVoteOfProxy.getDelegatedTo().equals(rightToVote)) return true;
		return thisWouldBeCircularDelegation(rightToVote, rightToVoteOfProxy.getDelegatedTo());
	}

	public boolean thisWouldBeCircularDelegation(AreaModel area, UserModel user, UserModel proxyToCheck) {
		if (area == null || user == null || proxyToCheck == null) return false;
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
	 *
	 * This will also accept all pending delegation requests. (User may already be a public proxy.)
	 *
	 * @param publicProxy the voter that wants to become a public proxy
	 * @param area voter will become a public proxy in an area
	 * @param proxyVoterToken the users secret voterToken
	 * @return the proxy's new public checksum
	 * @throws LiquidoException when voterToken is invalid
	*/
	@Transactional
	public RightToVoteModel becomePublicProxy(UserModel publicProxy, AreaModel area, String proxyVoterToken) throws LiquidoException {
		log.trace("ENTER becomePublicProxy("+publicProxy.toStringShort()+", "+area+")");
		if (publicProxy == null) throw new IllegalArgumentException("need publixProxy to becomePublicProxy");
		if (area == null) throw new IllegalArgumentException("need area to becomePublicProxy");
		RightToVoteModel publicProxyChecksum = castVoteService.isVoterTokenValid(proxyVoterToken);
		publicProxyChecksum.setPublicProxy(publicProxy);
		rightToVoteRepo.save(publicProxyChecksum);  // MUST save before I can accept delegation requests
		acceptDelegationRequests(area, publicProxy, proxyVoterToken);
		log.info("becomePublicProxy: "+publicProxy+ " is now a public proxy");
		return rightToVoteRepo.save(publicProxyChecksum);
	}

	/**
	 * Get the checksum of a public proxy, so that a voter can delegate to it.
	 * This can also be used to check if a user alreayd is a public proxy.
	 * @param area Area
	 * @param proxy proxy to check
	 * @return (optionally) the checksum of a public proxy
	 */
	public Optional<RightToVoteModel> getRightToVoteOfPublicProxy(AreaModel area, UserModel proxy) {
		return rightToVoteRepo.findByAreaAndPublicProxy(area, proxy);
	}

	public List<DelegationModel> findAcceptedDirectDelegations(AreaModel area, UserModel proxy) {
		return delegationRepo.findAcceptedDelegations(area, proxy);
	}

	public List<DelegationModel> findDelegationRequests(AreaModel area, UserModel proxy) {
		return delegationRepo.findDelegationRequests(area, proxy);
	}

	/**
	 * Get the delegation from this voter to his proxy in this area.
	 * This MAY also be a requested delegation.
	 * @param area an area
	 * @param voter a voter that may have a proxy assigned in area
	 * @return (optionally) the delegation from this voter to his proxy.
	 */
	public Optional<DelegationModel> getDelegationToDirectProxy(AreaModel area, UserModel voter) {
		return delegationRepo.findByAreaAndFromUser(area, voter);
	}

	/**
	 * Recursively count number of accepted delegations to a proxy.
	 * The proxy may vote that many times plus his own vote.
	 *
	 * See also {@link #findAcceptedDirectDelegations(AreaModel, UserModel)} which uses {@link DelegationModel} to count delegations.
	 *
	 * @param proxiesVoterToken the voterToken of a proxy
	 * @return the number of delegations to this proxy. The proxies vote conts that many times plus his own vote.
	 */
	public long getRecursiveDelegationCount(String proxiesVoterToken) throws LiquidoException {
		RightToVoteModel proxiesRightToVote = castVoteService.isVoterTokenValid(proxiesVoterToken);
		return countDelegationsRec(proxiesRightToVote, 0);
	}

	/**
	 * Recursively count the number accepted delegations that a proxy has received.
	 * This recursive method is meant to be called with includeNonTransitiveDelegations = true and depth = 0, because non transitive delegations
	 * do count on depth == 0. They are direct delegations. On deeper levels, only transitive delegations are added up.
	 *
	 * @param rightToVote a rightToVote that may have delegations to it
	 * @param depth current recursion depth
	 * @return the number of delegations to this proxy (without the proxies own vote)
	 */
	private long countDelegationsRec(RightToVoteModel rightToVote, long depth) {
		if (rightToVote == null) throw new IllegalArgumentException("rightToVote must not be null to count delegations");
		if (depth > Long.MAX_VALUE - 10) throw new RuntimeException("There seems to be a circular delegation with rightToVote: "+rightToVote);   // MUST prevent server from crashing!
		long numDelegations = 0;
		List<RightToVoteModel> delegations = rightToVoteRepo.findByDelegatedTo(rightToVote);
		for (RightToVoteModel delegatedChecksum: delegations) {
			numDelegations += 1 + countDelegationsRec(delegatedChecksum, depth+1);
		}
		return numDelegations;
	}

	/**
	 * GIVEN a proxy that has pending delegation requests
	 *  WHEN this proxy accepts all the delegation requests to him,
	 *  THEN all delegations are assigned (DelegationModel and Checksum delegation)
	 *   AND the requests are cleared.
	 * @param area area for delegation
	 * @param proxy the proxy that accepts all pending delegation requests to him
	 * @param proxyVoterToken necessary to accept delegations and assign proxy
	 * @return the newly counted number of votes that his proxy now may cast.
	 * @throws LiquidoException when one of the delegations cannot be assigned
	 */
	@Transactional
	public long acceptDelegationRequests(AreaModel area, UserModel proxy, String proxyVoterToken) throws LiquidoException {
		log.debug("=> accept delegation requests for proxy "+proxy.toStringShort()+" in area.id="+area.getId());
		List<DelegationModel> delegationRequests = delegationRepo.findDelegationRequests(area, proxy);
		Optional<RightToVoteModel> proxyChecksum = Optional.of(castVoteService.isVoterTokenValid(proxyVoterToken));
		for(DelegationModel delegation: delegationRequests) {
			log.trace("Accepting delegation request from "+delegation.getFromUser()+" to proxy "+delegation.getToProxy());
			this.assignProxy(area, delegation.getFromUser(), proxy,	delegation.getRequestedDelegationFrom(), proxyChecksum);
		}
		long delegationCount = getRecursiveDelegationCount(proxyVoterToken);
		log.info("<= accepted "+delegationRequests.size()+" delegation requests for proxy "+proxy.toStringShort()+" in area.id="+area.getId()+ ", new delegationCount="+delegationCount);
		return delegationCount;
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
			UserModel directProxy = delegation.get().getToProxy();
			Optional<UserModel> topProxy = findTopProxy(area, directProxy);
			return topProxy.isPresent() ? topProxy : Optional.of(directProxy);
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
		RightToVoteModel rightToVoteModel = castVoteService.isVoterTokenValid(voterToken);

		//----- remove the delegation of the voter's checksum
		if (rightToVoteModel != null) {
			rightToVoteModel.setDelegatedTo(null);
			rightToVoteRepo.save(rightToVoteModel);
		}

		//----- delete the DelegationModel
		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		if (delegation.isPresent()) {
			delegationRepo.delete(delegation.get());
		}
	}

	//TODO: delete (public) proxy: remove all delegations

}
