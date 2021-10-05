package UART

import chisel3._
import chisel3 . util._
import org.scalatest._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation
import caravan.bus.wishbone.WishboneConfig
import caravan.bus.tilelink.TilelinkConfig
import caravan.bus.tilelink._
import jigsaw._

class uartHarnessTest extends FreeSpec with ChiselScalatestTester {

  "UART Harness TEST" in {
    implicit val config = WishboneConfig(32,32)
    test(new uartHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>    

        c.io.req.bits.isWrite.poke(0.B)
        c.clock.step(1)

        c.io.req.bits.addrRequest.poke(0.U)
        c.clock.step(1)
        c.io.req.bits.dataRequest.poke(87.U)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(1.B)
        c.io.req.valid.poke(1.B)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(0.B)
        c.io.req.valid.poke(0.B)
        c.clock.step(1)

        c.io.req.bits.addrRequest.poke(4.U)
        c.clock.step(1)
        c.io.req.bits.dataRequest.poke("b10101010".U)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(1.B)
        c.io.req.valid.poke(1.B)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(0.B)
        c.io.req.valid.poke(0.B)
        c.clock.step(1)

        c.io.req.bits.addrRequest.poke(16.U)
        c.clock.step(1)
        c.io.req.bits.dataRequest.poke(1.U)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(1.B)
        c.io.req.valid.poke(1.B)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(0.B)
        c.io.req.valid.poke(0.B)
        c.clock.step(1)

        c.io.req.bits.addrRequest.poke(16.U)
        c.clock.step(1)
        c.io.req.bits.dataRequest.poke(0.U)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(1.B)
        c.io.req.valid.poke(1.B)
        c.clock.step(1)
        c.io.req.bits.isWrite.poke(0.B)
        c.io.req.valid.poke(0.B)
        c.clock.step(1)

        c.clock.step(900)



    }
  }
}