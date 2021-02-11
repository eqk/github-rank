package service

import models.{Contributor, Repository}

trait ContributorsService[F[_]] {
  def getRepositories(orgName: String): F[List[Repository]]
  def getContributors(orgName: String, repository: Repository): F[List[Contributor]]
}
