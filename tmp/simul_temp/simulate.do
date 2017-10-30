
vlib work

# Include files and compile them
vcom "../../RTL/Router/arbiter_in.vhd"
vcom "../../RTL/Router/arbiter_out.vhd"
vcom "../../RTL/Router/allocator.vhd"
vcom "../../RTL/Router/LBDR.vhd"
vcom "../../RTL/Router/xbar.vhd"
vcom "../../RTL/Router/FIFO_one_hot_credit_based.vhd"
vcom "../../RTL/Router/Router_32_bit_credit_based.vhd"
vcom "../../Test/credit_based/TB_package_32_bit_Tron.vhd"
vcom "../../Test/credit_based/network_2x2_NI_Test_Rand_credit_based_tb_tron.vhd"

# Start the simulation
vsim work.tb_network_2x2

# Draw waves
do wave_2x2.do
# Run the simulation
vcd file wave.vcd
vcd add -r -optcells /*
run 20 ms
vcd flush
