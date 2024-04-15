package aws.retrospective.service;


import aws.retrospective.dto.CreateRetrospectiveDto;
import aws.retrospective.dto.CreateRetrospectiveResponseDto;
import aws.retrospective.dto.GetRetrospectivesDto;
import aws.retrospective.dto.PaginationResponseDto;
import aws.retrospective.dto.RetrospectiveResponseDto;
import aws.retrospective.dto.RetrospectiveType;
import aws.retrospective.dto.RetrospectivesOrderType;
import aws.retrospective.dto.UpdateRetrospectiveDto;
import aws.retrospective.entity.Retrospective;
import aws.retrospective.entity.RetrospectiveTemplate;
import aws.retrospective.entity.Team;
import aws.retrospective.entity.User;
import aws.retrospective.entity.UserTeam;
import aws.retrospective.repository.RetrospectiveRepository;
import aws.retrospective.repository.RetrospectiveTemplateRepository;
import aws.retrospective.repository.TeamRepository;
import aws.retrospective.repository.UserRepository;
import aws.retrospective.repository.UserTeamRepository;
import aws.retrospective.specification.RetrospectiveSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RetrospectiveTemplateRepository templateRepository;
    private final BookmarkService bookmarkService;
    private final UserTeamRepository userTeamRepository;

    @Transactional(readOnly = true)
    public PaginationResponseDto<RetrospectiveResponseDto> getRetrospectives(
        GetRetrospectivesDto dto) {
        Sort sort = getSort(dto.getOrder());
        PageRequest pageRequest = PageRequest.of(dto.getPage(), dto.getSize(), sort);

        Specification<Retrospective> spec = Specification.where(
                RetrospectiveSpecification.withKeyword(dto.getKeyword()))
            .and(RetrospectiveSpecification.withUserId(dto.getUserId()))
            .and(RetrospectiveSpecification.withBookmark(dto.getIsBookmarked(), dto.getUserId()));

        Page<Retrospective> page = retrospectiveRepository.findAll(spec, pageRequest);

        boolean hasBookmarksByUser = page.stream()
            .anyMatch(retrospective -> hasBookmarksByUser(retrospective, dto.getUserId()));

        return PaginationResponseDto.fromPage(page,
            retrospective -> RetrospectiveResponseDto.of(retrospective, hasBookmarksByUser));
    }

    @Transactional
    public RetrospectiveResponseDto updateRetrospective(Long retrospectiveId,
        UpdateRetrospectiveDto dto) {
        Retrospective retrospective = retrospectiveRepository.findById(retrospectiveId).orElseThrow(
            () -> new EntityNotFoundException("Not found retrospective: " + retrospectiveId));

        retrospective.update(dto.getTitle(), dto.getStatus(), dto.getThumbnail(),
            dto.getDescription());

        boolean hasBookmarksByUser = hasBookmarksByUser(retrospective, dto.getUserId());

        return RetrospectiveResponseDto.of(retrospective, hasBookmarksByUser);
    }

    private boolean hasBookmarksByUser(Retrospective retrospective, Long userId) {
        return retrospective.getBookmarks().stream()
            .anyMatch(bookmark -> bookmark.getUser().getId().equals(userId));
    }


    private Sort getSort(RetrospectivesOrderType orderType) {
        if (orderType == RetrospectivesOrderType.OLDEST) {
            return Sort.by(Direction.ASC, "createdDate");
        }

        return Sort.by(Direction.DESC, "createdDate");
    }

    @Transactional
    public CreateRetrospectiveResponseDto createRetrospective(CreateRetrospectiveDto dto) {
        User user = findUserById(dto.getUserId());
        RetrospectiveTemplate template = findTemplateById(dto.getTemplateId());

        RetrospectiveType retrospectiveType = dto.getType();
        Team team = null;
        if (retrospectiveType == RetrospectiveType.TEAM) {
            team = createTeamWithUserId(dto.getUserId());
        }

        Retrospective retrospective = Retrospective.builder().title(dto.getTitle())
            .status(dto.getStatus()).team(team).user(user).template(template)
            .thumbnail(dto.getThumbnail()).startDate(dto.getStartDate())
            .description(dto.getDescription()).build();

        Retrospective savedRetrospective = retrospectiveRepository.save(retrospective);

        return toResponseDto(savedRetrospective);
    }

    @Transactional
    public void deleteRetrospective(Long retrospectiveId, Long userId) {
        Retrospective retrospective = retrospectiveRepository.findById(retrospectiveId).orElseThrow(
            () -> new EntityNotFoundException("Not found retrospective: " + retrospectiveId));

        if (!retrospective.isOwnedByUser(userId)) {
            throw new IllegalArgumentException(
                "Not allowed to delete retrospective: " + retrospectiveId);
        }

        retrospectiveRepository.deleteById(retrospectiveId);
    }

    public boolean toggleBookmark(Long retrospectiveId, Long userId) {
        return bookmarkService.toggleBookmark(userId, retrospectiveId);
    }


    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Not found user: " + userId));
    }

    private RetrospectiveTemplate findTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("Not found template: " + templateId));
    }

    private Optional<Team> findTeamByIdOptional(Long teamId) {
        return (teamId != null) ? teamRepository.findById(teamId) : Optional.empty();
    }

    public Team createTeamWithUserId(Long userId) {
        Team team = teamRepository.save(Team.builder().build());
        User user = findUserById(userId);

        userTeamRepository.save(UserTeam.builder()
            .team(team)
            .user(user)
            .build());

        return team;
    }


    private CreateRetrospectiveResponseDto toResponseDto(Retrospective retrospective) {
        return CreateRetrospectiveResponseDto.builder().id(retrospective.getId())
            .title(retrospective.getTitle())
            .teamId(Optional.ofNullable(retrospective.getTeam()).map(Team::getId).orElse(null))
            .userId(retrospective.getUser().getId()).templateId(retrospective.getTemplate().getId())
            .status(retrospective.getStatus()).thumbnail(retrospective.getThumbnail())
            .description(retrospective.getDescription()).startDate(retrospective.getStartDate())
            .build();
    }

}
