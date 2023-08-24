# GitHub Client API

This project is a simple API client for interacting with the GitHub API to retrieve user repositories and related information.

## Features

- Fetch and list GitHub repositories for a specific user.
- Retrieve repository details, including branches and their last commit SHA.

Endpoints
GET /repositories?username={username}
Retrieves a list of GitHub repositories for the specified username.

GET /repositories?username=kubajaslan

Response
If successful, you will receive a JSON response containing a list of repositories with their details.

Example Response:

[
    {
        "repositoryName": "example-repo",
        "ownerLogin": "kubajaslan",
        "branches": [
              {
                "branchName": "main",
                "lastCommitSHA": "abc123"
            }
        ]
    }
]
