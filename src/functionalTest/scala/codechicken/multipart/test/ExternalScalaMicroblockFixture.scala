package codechicken.multipart.test

import codechicken.microblock.Microblock

/** Minimal external Scala trait matching ProjectRed's MicroblockGenerator
  * registration path.
  */
trait ExternalScalaMicroblockFixture extends Microblock {
  private var state = 41

  def fixtureState = state

  override def shouldRenderDynamic = true

  override def getLightValue = state
}
