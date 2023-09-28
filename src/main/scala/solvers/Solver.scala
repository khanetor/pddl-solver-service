package solvers

import cats.effect.{IO, Resource}

import fr.uga.pddl4j.problem.operator.Action
import fr.uga.pddl4j.planners.LogLevel

trait Solver:
  def solve(domainText: String, problemText: String): IO[Solver.Solution]
  def exit(): IO[Unit]

object Solver:
  def apply(timeout: Int): Resource[IO, Solver] =
    Resource.make(
      IO(
        Pddl4jSolver(timeout, LogLevel.INFO)
      )
    )(_.exit())

  case class Solution(plan: List[String], states: List[String])

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
