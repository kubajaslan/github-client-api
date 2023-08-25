package com.example.githubclientapi.service;

import com.example.githubclientapi.entity.Branch;
import com.example.githubclientapi.entity.GitHubRepository;
import com.example.githubclientapi.entity.Owner;
import com.example.githubclientapi.exception.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
                        .bodyToFlux(GitHubRepository.class)
                        .filter(repo -> !repo.fork())
                        .flatMap(repo -> fetchBranchesForRepository(repo.owner(), repo.name(), repo.fork()))
                        .collectList()
                        .onErrorMap(WebClientResponseException.class, this::handleWebClientException);
    }



    private Mono<GitHubRepository> fetchBranchesForRepository(Owner owner, String
            repoName, boolean fork) {
        return webClient.get()
                        .uri("/repos/{owner}/{repoName}/branches", owner.login(), repoName)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToFlux(Branch.class)
                        .flatMap(branch -> fetchLastCommitSHA(owner.login(), repoName,
                                branch.name())
                                .map(lastCommitSHA -> new Branch(branch.name(), lastCommitSHA)))
                        .collectList()
                        .map(branches -> new GitHubRepository(repoName, owner, fork, branches));
    }

    private Mono<String> fetchLastCommitSHA(String owner, String repoName, String branchName) {
        return webClient.get()
                        .uri("/repos/{owner}/{repoName}/commits/{branchName}", owner, repoName,
                                branchName)
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(jsonNode -> jsonNode.get("sha").asText());
    }

    private Throwable handleWebClientException(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new NotFoundException("User not found");
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new ForbiddenException("You might have exceeded the request limit.");
        } else if (ex.getStatusCode() == HttpStatus.NOT_ACCEPTABLE) {
            return new NotAcceptableException("Not acceptable");
        } else if (ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            return new InternalServerErrorException("Internal error, you might have exceeded the request limit");
        } else {
            return ex; // If none of the expected status codes match, rethrow the original exception.
        }
    }
}
