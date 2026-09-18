package com.peerview.peers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PeerRepository extends JpaRepository<Peer, UUID> {
    boolean existsByUserIdAndPeerId(UUID userId, UUID peerId);
    long countByUserId(UUID userId);
    @Query("select p from Peer p join fetch p.peer where p.user.id = :userId order by p.createdAt desc")
    List<Peer> findAllByUserId(UUID userId);
}
