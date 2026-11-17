package codechicken.multipart.test

import cpw.mods.fml.relauncher.{Side, SideOnly}

trait RegistrationParentA { def parentA = 11 }
trait RegistrationParentB { def parentB = 22 }
trait RegistrationInterface { def abstractOnly: Int }

trait ScalaTraitRegistrationFixture
    extends RegistrationParentA
    with RegistrationParentB
    with RegistrationInterface {
  var count = 7
  private var hidden = 9
  def concrete = count + hidden
  def overloaded(value: Int) = value + 1
  def overloaded(value: java.lang.String) = value.length
  protected def protectedMethod = 3
  private def privateMethod = 4
  def deferred: Int
  override def toString = super.toString + "fixture"
  @SideOnly(Side.CLIENT) def clientOnly = 31
  @SideOnly(Side.SERVER) def serverOnly = 32
}

trait RegistrationAliasFixture {
  def alias(value: String) = value.length
}
