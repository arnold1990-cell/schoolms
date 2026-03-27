package com.schoolms.term;

import com.schoolms.academicsession.AcademicSession;
import com.schoolms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Term extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean active;
    @ManyToOne(optional = false)
    private AcademicSession academicSession;
}
