
vlib work

# Include files and compile them
vcom "../../RTL/Router/arbiter_in.vhd"
vcom "../../RTL/Router/arbiter_out.vhd"
vcom "../../RTL/Router/allocator.vhd"
vcom "../../RTL/Router/LBDR.vhd"
vcom "../../RTL/Router/xbar.vhd"
vcom "../../RTL/Router/FIFO_one_hot_credit_based.vhd"
vcom "../../RTL/Router/Router_32_bit_credit_based.vhd"
vcom "TB_package_for_reading_file and_writting_file_back.vhd"
vcom "network_2x2_NI_Test_Rand_credit_based_tb_tron.vhd"
vcom "network_2x2_NI_credit_based.vhd"

# Start the simulation
vsim work.tb_network_2x2

# Draw waves
do wave_2x2.do
# Run the simulation
vcd file wave.vcd
vcd add -r -optcells /*
run 20 ms
vcd flush
