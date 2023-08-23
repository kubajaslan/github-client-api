package com.example.githubclientapi.controller;

import com.example.githubclientapi.entity.GitHubRepository;
import com.example.githubclientapi.service.GitHubAPIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class GitHubRepositoryController {

    private final GitHubAPIService gitHubAPIService;

    public GitHubRepositoryController(GitHubAPIService gitHubAPIService) {
        this.gitHubAPIService = gitHubAPIService;
    }

    @GetMapping("/repositories")
    public Mono<List<GitHubRepository>> getRepositories(@RequestParam String username) {
        return gitHubAPIService.getUserRepositories(username);
    }
}

