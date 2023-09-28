package solvers

import solvers.Solver
import fr.uga.pddl4j.problem.operator.Action
import cats.effect.IO

import enhsp2.ENHSP

class ENHSP20Solver extends Solver {
  override def solve(
      domainText: String,
      problemText: String
  ): IO[Solver.Solution] = for
    planner <- IO(ENHSP(false))
    _ <- IO.blocking(planner.parseInput(ENHSP20Solver.args))
    _ <- IO(planner.configurePlanner())
    _ <- IO.blocking(planner.parsingDomainAndProblem(ENHSP20Solver.args))
    _ <- IO(planner.planning())
  yield Solver.Solution(List.empty, List.empty)

  override def exit(): IO[Unit] = IO.unit
}

object ENHSP20Solver:
  val args =
    "-o /home/kha/Projects/meyer/planner/meyer.domain.pddl -f /home/kha/Projects/meyer/planner/meyer.problem.pddl /tmp/domain--4202-DI4SwF9PTD23-.pddl /tmp/problem--4202-eSdbrC0Aduvu-.pddl"
      .split(" ")
