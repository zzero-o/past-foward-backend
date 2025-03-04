package aws.retrospective.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Retrospective extends BaseEntity {

    @Id
    @Column(name = "retrospective_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String title; // 회고 제목

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team; // 회고를 작성한 팀 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotNull
    private User user; // 회고를 작성한 사용자 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "template_id")
    private RetrospectiveTemplate template; // 회고 템플릿 정보

    @Enumerated(value = EnumType.STRING)
    private ProjectStatus status; // 회고 진행 상태(시작 전, 진행 중, 완료)

    @OneToMany(mappedBy = "retrospective")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder
    public Retrospective(String title, ProjectStatus status, Team team, User user,
        RetrospectiveTemplate template) {
        this.title = title;
        this.status = status;
        this.team = team;
        this.user = user;
        this.template = template;
    }
}