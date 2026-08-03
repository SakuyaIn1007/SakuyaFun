package com.sakuya.backend.friend;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface FriendRequestRepository extends JpaRepository<FriendRequest,UUID>{List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(UUID receiverId,FriendRequest.Status status); boolean existsBySenderIdAndReceiverIdAndStatus(UUID sender,UUID receiver,FriendRequest.Status status);}
