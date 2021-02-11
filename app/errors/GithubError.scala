package errors

sealed trait AppError extends RuntimeException
sealed trait GithubError extends AppError

case object NotFoundError extends GithubError
case object RateLimitExceeded extends GithubError
