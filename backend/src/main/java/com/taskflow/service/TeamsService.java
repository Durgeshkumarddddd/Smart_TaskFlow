package com.taskflow.service;

import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Team;
import com.taskflow.model.TeamMember;
import com.taskflow.model.TeamMemberId;
import com.taskflow.model.User;
import com.taskflow.repository.TeamMemberRepository;
import com.taskflow.repository.TeamRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamsService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')") 
    public Team createTeam(String name, String description) {
        User user = getCurrentUser();
        if (user.getRole() != com.taskflow.model.Role.ADMIN && user.getRole() != com.taskflow.model.Role.MANAGER) {
            throw new ForbiddenException("Only Admin or Manager can create teams");
        }
        if (teamRepository.findAll().stream().anyMatch(t -> t.getName().equalsIgnoreCase(name))) {
            throw new BadRequestException("Team with this name already exists");
        }
        Team team = Team.builder()
                .name(name)
                .description(description)
                .manager(user)
                .build();
        Team saved = teamRepository.save(team);
        // add manager as member automatically
        TeamMember tm = TeamMember.builder()
                .id(new TeamMemberId(saved.getId(), user.getId()))
                .team(saved)
                .user(user)
                .build();
        teamMemberRepository.save(tm);
        return saved;
    }

    public List<Team> getTeamsForCurrentUser() {
        User user = getCurrentUser();
        if (user.getRole() == com.taskflow.model.Role.ADMIN) {
            return teamRepository.findAll();
        } else if (user.getRole() == com.taskflow.model.Role.MANAGER) {
            // Include teams where they are the manager
            return teamRepository.findByManager(user);
        } else {
            // Find teams where user is an explicit member
            return teamMemberRepository.findByUserId(user.getId()).stream()
                    .filter(tm -> tm.getTeam() != null)
                    .map(tm -> tm.getTeam())
                    .collect(Collectors.toList());
        }
    }

    public Team getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        User user = getCurrentUser();
        if (user.getRole() == com.taskflow.model.Role.ADMIN) return team;
        if (user.getRole() == com.taskflow.model.Role.MANAGER && team.getManager().getId().equals(user.getId())) return team;
        boolean member = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(tm -> tm.getUser().getId().equals(user.getId()));
        if (!member) throw new ForbiddenException("Not part of this team");
        return team;
    }

    public boolean isUserInTeam(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(tm -> tm.getUser().getId().equals(userId));
    }

    @Transactional
    public void addMember(Long teamId, Long userId) {
        User requesting = getCurrentUser();
        Team team = getTeamById(teamId);
        if (!(requesting.getRole() == com.taskflow.model.Role.ADMIN ||
              (requesting.getRole() == com.taskflow.model.Role.MANAGER &&
               team.getManager().getId().equals(requesting.getId())))) {
            throw new ForbiddenException("Only Admin or team Manager can add members");
        }
        User toAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        TeamMemberId id = new TeamMemberId(teamId, userId);
        if (teamMemberRepository.existsById(id)) return;
        TeamMember tm = TeamMember.builder()
                .id(id)
                .team(team)
                .user(toAdd)
                .build();
        teamMemberRepository.save(tm);
    }

    @Transactional
    public void removeMember(Long teamId, Long userId) {
        User requesting = getCurrentUser();
        Team team = getTeamById(teamId);
        if (!(requesting.getRole() == com.taskflow.model.Role.ADMIN ||
              (requesting.getRole() == com.taskflow.model.Role.MANAGER &&
               team.getManager().getId().equals(requesting.getId())))) {
            throw new ForbiddenException("Only Admin or team Manager can remove members");
        }
        if (team.getManager().getId().equals(userId)) {
            throw new BadRequestException("Cannot remove the manager from the team");
        }
        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        User requesting = getCurrentUser();
        Team team = getTeamById(teamId);
        if (!(requesting.getRole() == com.taskflow.model.Role.ADMIN ||
              (requesting.getRole() == com.taskflow.model.Role.MANAGER &&
               team.getManager().getId().equals(requesting.getId())))) {
            throw new ForbiddenException("Only Admin or team Manager can delete the team");
        }
        teamRepository.delete(team);
    }

    @Transactional
    public void updateMemberLeadership(Long teamId, Long userId, boolean isLeader) {
        User requesting = getCurrentUser();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        
        // Authorization check: Only Admin or the Team's Manager can promote/demote
        if (!(requesting.getRole() == com.taskflow.model.Role.ADMIN ||
              (requesting.getRole() == com.taskflow.model.Role.MANAGER &&
               team.getManager().getId().equals(requesting.getId())))) {
            throw new ForbiddenException("Only Admin or team Manager can update leadership status");
        }

        TeamMember tm = teamMemberRepository.findById(new TeamMemberId(teamId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team"));
        
        tm.setLeader(isLeader);
        teamMemberRepository.save(tm);
    }
}
