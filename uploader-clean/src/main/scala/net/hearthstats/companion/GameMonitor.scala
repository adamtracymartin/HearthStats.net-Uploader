package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.concurrent.duration.DurationInt
import net.hearthstats.ProgramHelper
import net.hearthstats.config.UserConfig
import net.hearthstats.game.GameEvent
import net.hearthstats.game.Screen._
import net.hearthstats.game.ScreenEvent
import rx.lang.scala.Observable
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import net.hearthstats.core.GameMode._
import net.hearthstats.game.imageanalysis.LobbyAnalyser
import grizzled.slf4j.Logging
import net.hearthstats.game.imageanalysis.Casual
import net.hearthstats.game.imageanalysis.Ranked
import net.hearthstats.game.ScreenGroup

class GameMonitor(
  programHelper: ProgramHelper,
  config: UserConfig,
  companionState: CompanionState,
  lobbyAnalyser: LobbyAnalyser,
  imageToEvent: ImageToEvent) extends Logging {

  import lobbyAnalyser._

  val gameImages: Observable[BufferedImage] =
    Observable.interval(config.pollingDelayMs.get.millis).map { _ =>
      if (programHelper.foundProgram)
        Some(programHelper.getScreenCapture)
      else
        None
    }.filter(_.isDefined).map(_.get)

  val gameEvents: Observable[GameEvent] = gameImages.
    map(imageToEvent.eventFromImage).
    filter(_.isDefined).
    map(_.get)

  gameEvents.subscribe(handleGameEvent _)

  private def handleGameEvent(evt: GameEvent): Unit = {
    debug(evt)
    evt match {
      case s: ScreenEvent => handleScreenEvent(s)
    }
  }

  private def handleScreenEvent(evt: ScreenEvent) = {
    evt.screen match {
      case PLAY_LOBBY =>
        handlePlayLobby(evt)

      case PRACTICE_LOBBY if companionState.mode != Some(PRACTICE) =>
        info("Practice Mode detected")
        companionState.mode = Some(PRACTICE)

      case VERSUS_LOBBY if companionState.mode != Some(FRIENDLY) =>
        info("Versus Mode detected")
        companionState.mode = Some(FRIENDLY)

      case ARENA_LOBBY if companionState.mode != Some(ARENA) =>
        info("Arena Mode detected")
        companionState.mode = Some(ARENA)
        companionState.isNewArenaRun = isNewArenaRun(evt.image)

      case _ =>
        debug("no change in game mode")
    }
    if (evt.screen.group == ScreenGroup.PLAY) {
      val deckSlot = imageIdentifyDeckSlot(evt.image)
      if (deckSlot.isDefined && deckSlot != companionState.deckSlot) {
        info(s"deck ${deckSlot.get} detected")
        companionState.deckSlot = deckSlot
      }
    }
  }

  private def handlePlayLobby(evt: ScreenEvent): Unit = {
    mode(evt.image) match {
      case Some(Casual) if companionState.mode != Some(CASUAL) =>
        info("Casual Mode detected")
        companionState.mode = Some(CASUAL)
      case Some(Ranked) if companionState.mode != Some(RANKED) =>
        info("Ranked Mode detected")
        companionState.mode = Some(RANKED)
      case _ => // assuming no change in the mode
    }
    if (companionState.mode == Some(RANKED) && companionState.rank.isEmpty) {
      companionState.rank = lobbyAnalyser.analyzeRankLevel(evt.image)
    }
  }

}