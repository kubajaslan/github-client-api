package com.example.githubclientapi.service;

import com.example.githubclientapi.entity.Branch;
import com.example.githubclientapi.entity.GitHubRepository;
import com.example.githubclientapi.entity.Owner;
import com.example.githubclientapi.exception.ErrorResponseBody;
import com.example.githubclientapi.exception.ForbiddenException;
import com.example.githubclientapi.exception.NotAcceptableException;
import com.example.githubclientapi.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GitHubAPIService {

    private final WebClient webClient;

    public GitHubAPIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    @ExceptionHandler({NotFoundException.class, ForbiddenException.class, NotAcceptableException.class})
    public ResponseEntity<ErrorResponseBody> handleCustomExceptions(RuntimeException ex) {
        int status;
        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND.value();
        } else if (ex instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN.value();
        } else if (ex instanceof NotAcceptableException) {
            status = HttpStatus.NOT_ACCEPTABLE.value();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        ErrorResponseBody errorResponseBody = new ErrorResponseBody(status, ex.getMessage());
        return ResponseEntity.status(status).body(errorResponseBody);
    }

    public Mono<List<GitHubRepository>> getUserRepositories(String username) {
        return webClient.get()
                        .uri("/users/{username}/repos?per_page=100", username)
                        .header("Accept", "application/json")
                        .retrieve()
                        .onStatus(HttpStatus.NOT_FOUND::equals,
                                response -> Mono.error(new NotFoundException("User not found")))
                        .onStatus(HttpStatus.FORBIDDEN::equals,
                                response -> Mono.error(new ForbiddenException("You might have exceeded the request limit.")))
                        .onStatus(HttpStatus.NOT_ACCEPTABLE::equals,
                                response -> Mono.error(new NotAcceptableException("Not acceptable")))
                        .bodyToFlux(GitHubRepository.class)
                        .filter(repo -> repo.owner().login().equals(username))
                        .flatMap(repo -> fetchBranchesForRepository(repo.owner(), repo.name(), username))
                        .collectList();
    }

    private Mono<GitHubRepository> fetchBranchesForRepository(Owner owner, String repoName, String ownerLogin) {
        return webClient.get()
                        .uri("/repos/{owner}/{repoName}/branches", owner.login(), repoName)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToFlux(Branch.class)
                        .flatMap(branch -> fetchLastCommitSHA(owner.login(), repoName, branch.name())
                                .map(lastCommitSHA -> new Branch(branch.name(), lastCommitSHA)))
                        .collectList()
                        .map(branches -> new GitHubRepository(repoName, owner, branches));
    }

    private Mono<String> fetchLastCommitSHA(String owner, String repoName, String branchName) {
        return webClient.get()
                        .uri("/repos/{owner}/{repoName}/commits/{branchName}", owner, repoName, branchName)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(jsonNode -> jsonNode.get("sha").asText());
    }
}
