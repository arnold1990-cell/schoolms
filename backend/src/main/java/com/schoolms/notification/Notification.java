package com.schoolms.notification;

import com.schoolms.common.BaseEntity;
import com.schoolms.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Notification extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String body;
    private boolean read;
    @ManyToOne(optional = false)
    private User recipient;
}
