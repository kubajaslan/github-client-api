package com.example.githubclientapi.service;

import com.example.githubclientapi.entity.Branch;
import com.example.githubclientapi.entity.GitHubRepository;
import com.example.githubclientapi.entity.Owner;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubAPIService {

    private final WebClient webClient;

    public GitHubAPIService() {
        this.webClient = WebClient.create("https://api.github.com");
    }

    public Mono<List<GitHubRepository>> getUserRepositories(String username) {
        String url = "/users/" + username + "/repos?per_page=100";

        return webClient.get()
                        .uri(url)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToFlux(GitHubRepository.class)
//                        .filter(repo -> {
//                            System.out.println("owner login is = " + repo.getOwnerLogin());
//                            return !Objects.equals(repo.getOwnerLogin(), username);
//                        })
                        .flatMap(repo -> fetchBranchesForRepository(repo.getOwner(), repo.getName(),
                                repo.getOwnerLogin()))
                        .collectList();
    }

    private Mono<GitHubRepository> fetchBranchesForRepository(Owner owner, String repoName, String ownerLogin) {
        String url = "/repos/" + owner.getLogin() + "/" + repoName + "/branches";

        return webClient.get()
                        .uri(url)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToFlux(Branch.class)
                        .flatMap(branch -> fetchLastCommitSHA(owner.getLogin(), repoName, branch.getName())
                                .map(lastCommitSHA -> new Branch(branch.getName(), lastCommitSHA)))
                        .collectList()
                        .map(branches -> new GitHubRepository(repoName, owner, branches));
    }

    private Mono<String> fetchLastCommitSHA(String owner, String repoName, String branchName) {
        String url = "/repos/" + owner + "/" + repoName + "/commits/" + branchName;

        return webClient.get()
                        .uri(url)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(jsonNode -> jsonNode.get("sha").asText());
    }
}