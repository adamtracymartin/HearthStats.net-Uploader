package net.hearthstats.companion

import grizzled.slf4j.Logging
import net.hearthstats.core.GameMode
import net.hearthstats.core.Rank
import net.hearthstats.game.Screen
import net.hearthstats.modules.video.OngoingVideo

/**
 * Current perception of HearthStone game by the companion.
 */
class CompanionState extends Logging {
  debug("new CompanionState")

  var mode = GameMode.UNDETECTED
  var deckSlot: Option[Int] = None
  var rank: Option[Rank] = None
  var findingOpponent = false
  var isNewArenaRun = false
  var iterationsSinceClassCheckingStarted = 0
  var iterationsSinceYourTurn = 0
  var iterationsSinceOpponentTurn = 0
  var isYourTurn = false

  var ongoingVideo: Option[OngoingVideo] = None

  var lastScreen: Option[Screen] = None

  var iterationsSinceFindingOpponent = 0
  var iterationsSinceScreenMatched = 0
}

