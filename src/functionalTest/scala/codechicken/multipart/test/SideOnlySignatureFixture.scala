package codechicken.multipart.test

import cpw.mods.fml.relauncher.{Side, SideOnly}

/** Read as raw class bytes so the server side transformer cannot remove
  * annotations.
  */
class SideOnlySignatureFixture {
  @SideOnly(Side.CLIENT)
  def clientOnly = 1

  @SideOnly(Side.SERVER)
  def serverOnly = 2

  def common = 3
}
