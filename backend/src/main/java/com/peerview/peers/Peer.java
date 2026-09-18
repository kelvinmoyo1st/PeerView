package com.peerview.peers;

import com.peerview.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "peers", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"user_id", "peer_id"}))
public class Peer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peer_id", nullable = false)
    private User peer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Peer() {}

    public Peer(User user, User peer) {
        this.user = user;
        this.peer = peer;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public User getPeer() { return peer; }
    public Instant getCreatedAt() { return createdAt; }
}
