package solvers

import solvers.Solver

import cats.effect.{IO, Resource}
import java.nio.file.Files

import fr.uga.pddl4j.planners.LogLevel
import fr.uga.pddl4j.parser.Parser
import fr.uga.pddl4j.parser.DefaultParsedProblem
import fr.uga.pddl4j.planners.statespace.FF
import fr.uga.pddl4j.problem.DefaultProblem
import fr.uga.pddl4j.problem.operator.Action

import scala.jdk.CollectionConverters.*
import fr.uga.pddl4j.problem.State

class Pddl4jSolver(timeout: Int, logLevel: LogLevel, prefix: String = "")
    extends Solver:
  val parser = Parser()
  val planner = FF()
  planner.setLogLevel(logLevel)
  planner.setTimeout(timeout)

  def parse(domainText: String, problemText: String): IO[DefaultParsedProblem] =
    val resource = for
      domainFile <- Resource.make(
        IO.blocking(Files.createTempFile(prefix, "-domain.pddl"))
      )(f => IO.blocking(Files.delete(f)))
      problemFile <- Resource.make(
        IO.blocking(Files.createTempFile(prefix, "-problem.pddl"))
      )(f => IO.blocking(Files.delete(f)))
      _ <- Resource.eval(
        IO.blocking(Files.writeString(domainFile, domainText)) *>
          IO.blocking(Files.writeString(problemFile, problemText))
      )
    yield (domainFile, problemFile)

    resource.use:
      case (domainFile, problemFile) =>
        IO.blocking(parser.parse(domainFile.toFile(), problemFile.toFile()))

  override def solve(
      domainText: String,
      problemText: String
  ): IO[Solver.Solution] = for
    parsedProblem <- parse(domainText, problemText)
    problem <- IO(planner.instantiate(parsedProblem))
    plan <- IO(planner.solve(problem))
    steps = problem.toString(plan).split("\n").toList
    state = State(problem.getInitialState())
    states = plan.actions().asScala.toList.foldLeft(List(problem.toString(state)))((ss, a) =>
      state(a.getConditionalEffects())
      problem.toString(state) :: ss
    )
  yield Solver.Solution(steps, states)

  override def exit(): IO[Unit] = IO.unit
