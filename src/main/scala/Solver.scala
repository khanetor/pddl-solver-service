package solvers

import cats.effect.{Resource, IO}
import scala.jdk.CollectionConverters.*

import java.nio.file.{Files, Path}

import fr.uga.pddl4j.parser.{Parser, ParsedProblem, DefaultParsedProblem}
import fr.uga.pddl4j.planners.statespace.FF
import fr.uga.pddl4j.planners.LogLevel
import fr.uga.pddl4j.problem.operator.Action
import fr.uga.pddl4j.problem.DefaultProblem

class Solver(timeout: Int, logLevel: LogLevel, prefix: String = ""):
  val parser = Parser()
  val planner = FF()
  planner.setLogLevel(logLevel)
  planner.setTimeout(timeout)

  def parse(domainText: String, problemText: String): IO[DefaultParsedProblem] =
    val resource = for
      domain <- Resource.make(
        IO.blocking(Files.createTempFile(prefix, "-domain.pddl"))
      )(f => IO.blocking(Files.delete(f)))
      problem <- Resource.make(
        IO.blocking(Files.createTempFile(prefix, "-problem.pddl"))
      )(f => IO.blocking(Files.delete(f)))
      _ <- Resource.eval(
        IO.blocking(Files.writeString(domain, domainText)) *>
          IO.blocking(Files.writeString(problem, problemText))
      )
    yield (domain, problem)

    resource.use:
      case (domain, problem) =>
        IO.blocking(parser.parse(domain.toFile(), problem.toFile()))

  def solve(domainText: String, problemText: String): IO[List[Action]] =
    for
      parsedProblem <- parse(domainText, problemText)
      problem <- IO(DefaultProblem(parsedProblem))
      _ <- IO.blocking(problem.instantiate())
      plan <- IO.blocking(planner.solve(problem))
    yield plan.actions().asScala.toList

  def exit(): IO[Unit] = IO.unit

object Solver:
  def apply(timeout: Int): Resource[IO, Solver] =
    Resource.make(IO(new Solver(timeout, LogLevel.INFO)))(_.exit())

  val domainString = """
  (define (domain wgc)

  (:requirements
      :strips
      :typing
      :negative-preconditions
      :universal-preconditions)

  (:types
      package
      location
  )

  (:constants
      A B - location
  )

  (:predicates
      (at ?p - package ?loc - location)
      (boat_at ?loc - location)
      (can_eat ?p1 ?p2 - package)
      (onboard ?p - package)
      (boat_empty)
  )

  (:action load
      :parameters (?p - package ?loc - location)
      :precondition (and
          (boat_at ?loc)
          (at ?p ?loc)
          (boat_empty)
      )
      :effect (and
          (onboard ?p)
          (not (at ?p ?loc))
          (not (boat_empty))
      )
  )

  (:action unload
      :parameters (?p - package ?loc - location)
      :precondition (and
          (onboard ?p)
          (boat_at ?loc)
      )
      :effect (and
          (not (onboard ?p))
          (at ?p ?loc)
          (boat_empty)
      )
  )

  (:action move_boat
      :parameters (?from - location ?to - location)
      :precondition (and
          (boat_at ?from)
          (forall (?p1 ?p2 - package)
              (not (and
                  (at ?p1 ?from)
                  (at ?p2 ?from)
                  (can_eat ?p1 ?p2)
              ))
          )
      )
      :effect (and
          (not (boat_at ?from))
          (boat_at ?to)
      )
  )

  )
  """

  val problemString = """
  (define (problem wgc-1) (:domain wgc)
  (:objects
      wolf
      goat
      cabbage - package
  )

  (:init
      (boat_at A)
      (boat_empty)

      (at wolf A)
      (at goat A)
      (at cabbage A)

      (can_eat wolf goat)
      (can_eat goat cabbage)
  )

  (:goal (and
      (at wolf B)
      (at goat B)
      (at cabbage B)
  ))
  )
  """
