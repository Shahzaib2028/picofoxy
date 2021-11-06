import chisel3._
import buraq_mini.core.Core
import caravan.bus.common.{AddressMap, BusDecoder, Switch1toN}
import caravan.bus.wishbone.{Peripherals, WBRequest, WBResponse, WishboneConfig, WishboneDevice, WishboneErr, WishboneHost, WishboneMaster, WishboneSlave}
import chisel3.experimental.Analog
import chisel3.stage.ChiselStage
import jigsaw.fpga.boards.artyA7._
import jigsaw.rams.fpga.BlockRam
import jigsaw.peripherals.UART._

 
class Picofoxy(programFile: Option[String]) extends Module {
  val io = IO(new Bundle{
        
    //val gpio_io = Vec(4, Analog(1.W))
    val uart_tx_o = Output(Bool())
    val uart_intr_tx_o = Output(Bool())
    val uart_rx_i = Input(Bool())
  }) 

  val top = Module(new Top(programFile))
  val pll = Module(new PLL_8MHz())

  pll.io.clk_in1 := clock
  top.clock := pll.io.clk_out1

  io.uart_tx_o := top.io.uart_tx_o
  io.uart_intr_tx_o := top.io.uart_intr_tx_o
  top.io.uart_rx_i := io.uart_rx_i

}

//   val gpioInputWires = Wire(Vec(4, Bool()))
//   val gpioOutputWires = Wire(Vec(4, Bool()))
//   val gpioEnableWires = Wire(Vec(4, Bool()))

//   val gpioPads = TriStateBuffer(quantity=4)
//   val triStateBufferWires = for {
//     ((((a,b),c),d),e) <- gpioPads zip gpioInputWires zip gpioOutputWires zip gpioEnableWires zip io.gpio_io
//   } yield (a,b,c,d,e)

//   triStateBufferWires map { case(buf: IOBUF, in: Bool, out: Bool, en: Bool, io: Analog) => {
//     buf.io.connect(in, out, io, en)
//   }}

//   top.io.gpio_i := gpioInputWires.asUInt()
//   gpioOutputWires := top.io.gpio_o.asBools()
//   gpioEnableWires := top.io.gpio_en_o.asBools()

// }


class Top(programFile: Option[String]) extends Module {
  val io = IO(new Bundle {
    val uart_tx_o = Output(Bool())
    val uart_intr_tx_o = Output(Bool())
    val uart_rx_i = Input(Bool())
    //val gpio_intr_o = Output(UInt(32.W))
  })

  implicit val config: WishboneConfig = WishboneConfig(32, 32)
  val wb_imem_host = Module(new WishboneHost())
  val wb_imem_slave = Module(new WishboneDevice())
  val wb_dmem_host = Module(new WishboneHost())
  val wb_dmem_slave = Module(new WishboneDevice())
  val wb_uart_slave = Module(new WishboneDevice())
  val imem = Module(BlockRam.createNonMaskableRAM(programFile, bus=config, rows=1024))
  val dmem = Module(BlockRam.createMaskableRAM(bus=config, rows=1024))
  val uart = Module(new uart(new WBRequest(), new WBResponse()))
  val wbErr = Module(new WishboneErr())
  val core = Module(new Core())



  val addressMap = new AddressMap
  addressMap.addDevice(Peripherals.DCCM, "h40000000".U(32.W), "h00000FFF".U(32.W), wb_dmem_slave)
  addressMap.addDevice(Peripherals.GPIO, "h40001000".U(32.W), "h00000FFF".U(32.W), wb_uart_slave)
  val devices = addressMap.getDevices

  val switch = Module(new Switch1toN(new WishboneMaster(), new WishboneSlave(), devices.size))

  // WB <-> Core (fetch)
  wb_imem_host.io.reqIn <> core.io.imemReq
  core.io.imemRsp <> wb_imem_host.io.rspOut
  wb_imem_slave.io.reqOut <> imem.io.req
  wb_imem_slave.io.rspIn <> imem.io.rsp

  // WB <-> WB (fetch)
  wb_imem_host.io.wbMasterTransmitter <> wb_imem_slave.io.wbMasterReceiver
  wb_imem_slave.io.wbSlaveTransmitter <> wb_imem_host.io.wbSlaveReceiver

  // WB <-> Core (memory)
  wb_dmem_host.io.reqIn <> core.io.dmemReq
  core.io.dmemRsp <> wb_dmem_host.io.rspOut
  wb_dmem_slave.io.reqOut <> dmem.io.req
  wb_dmem_slave.io.rspIn <> dmem.io.rsp


  // Switch connection
  switch.io.hostIn <> wb_dmem_host.io.wbMasterTransmitter
  switch.io.hostOut <> wb_dmem_host.io.wbSlaveReceiver
  for (i <- 0 until devices.size) {
    switch.io.devIn(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[WishboneDevice].io.wbSlaveTransmitter
    switch.io.devOut(devices(i)._2.litValue().toInt) <> devices(i)._1.asInstanceOf[WishboneDevice].io.wbMasterReceiver
  }
  switch.io.devIn(devices.size) <> wbErr.io.wbSlaveTransmitter
  switch.io.devOut(devices.size) <> wbErr.io.wbMasterReceiver
  switch.io.devSel := BusDecoder.decode(wb_dmem_host.io.wbMasterTransmitter.bits.adr, addressMap)


  wb_uart_slave.io.reqOut <> uart.io.request
  wb_uart_slave.io.rspIn <> uart.io.response

  io.uart_tx_o := uart.io.cio_uart_tx_o
  io.uart_intr_tx_o := uart.io.cio_uart_intr_tx_o
  uart.io.cio_uart_rx_i := io.uart_rx_i

  core.io.stall_core_i := false.B
  core.io.irq_external_i := false.B



}
object PicofoxyDriver extends App {
  (new ChiselStage).emitVerilog(new Picofoxy(None))
}
