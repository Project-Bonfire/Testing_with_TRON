--We read output file here generated by Tron adapter and read the source, destination and body.
library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
use IEEE.NUMERIC_STD.all;
 use ieee.math_real.all;
 use std.textio.all;
 use ieee.std_logic_misc.all;

package TB_Package is


  procedure credit_counter_control(signal clk: in std_logic;
                                   signal credit_in: in std_logic;
				   signal valid_out: in std_logic;
                                   signal credit_counter_out: out std_logic_vector(1 downto 0));

  procedure gen_packet_from_file(network_size, frame_length, initial_delay: in integer;
				  finish_time: in time;
          stim_file: in  string;
                                  signal clk: in std_logic;
                                  signal credit_counter_in: in std_logic_vector(1 downto 0);
                                  signal valid_out: out std_logic;
                                  signal port_in: out std_logic_vector);

  procedure get_packet(DATA_WIDTH, initial_delay: in integer;
		       signal clk: in std_logic;
                       signal credit_out: out std_logic;
		       signal valid_in: in std_logic;
		       signal port_in: in std_logic_vector);

end TB_Package;

package body TB_Package is

  constant Header_type : std_logic_vector := "001";
  constant Body_type   : std_logic_vector := "010";
  constant Tail_type   : std_logic_vector := "100";
  constant Packet_length : integer := 3;


procedure credit_counter_control(
	  signal clk: in std_logic;
          signal credit_in : in std_logic;
          signal valid_out : in std_logic;
          signal credit_counter_out : out std_logic_vector(1 downto 0)) is


    variable credit_counter: std_logic_vector (1 downto 0);
    begin
    credit_counter := "11";

  while true loop
      credit_counter_out<= credit_counter;
      wait until clk'event and clk ='1';
      if valid_out = '1' and credit_in ='1' then
        credit_counter := credit_counter;
      elsif credit_in = '1' then
        credit_counter := credit_counter + 1;
      elsif valid_out = '1' and  credit_counter > 0 then
        credit_counter := credit_counter - 1;
      else
        credit_counter := credit_counter;
      end if;
    end loop;
  end credit_counter_control;


  procedure gen_packet_from_file(network_size, frame_length, initial_delay : in integer;
                      finish_time: in time;
                      stim_file: in  string;
                      signal clk: in std_logic;
                      signal credit_counter_in: in std_logic_vector(1 downto 0);
                      signal valid_out: out std_logic;
                      signal port_in : out std_logic_vector
                       ) is

                      variable values_bv : bit_vector(61 downto 0); -- we can change the format of values datatype as per the type provided by the adapter output file
                      variable values : std_logic_vector(61 downto 0); -- we can change the format of values datatype as per the type provided by the adapter output file
                      variable destination_id: integer;
                      variable source_id: integer;
                      variable body_data: integer;
                      variable tail_data: integer;
                      variable frame_starting_delay,frame_ending_delay: integer:= 0;
                      variable credit_counter: std_logic_vector (1 downto 0);
		      variable INPUTLINE : line;
          file file_handler : text open read_mode is stim_file;

begin
    while not endfile(file_handler) loop
      readline(file_handler, INPUTLINE);
      read(INPUTLINE, values_bv);
      values := To_StdLogicVector(values_bv);
-- store 2 bit values
      source_id := to_integer(unsigned(values(61 downto 60)));
--store 2 bit values
      destination_id := to_integer(unsigned(values(59 downto 58)));
-- store 32 bit values
      body_data := to_integer(unsigned(values(57 downto 29)));
-- store 32 bit values
      tail_data := to_integer(unsigned(values(28 downto 0)));

----------------------------------------------------------------------
      valid_out <= '0';
      port_in <= "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" ;
      wait until clk'event and clk ='1';
      for i in 0 to initial_delay loop
      wait until clk'event and clk ='1';
      end loop;
      port_in <= "UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU" ;

-----------------------------------------------------------------

      wait until clk'event and clk ='0';
      valid_out <= '0';

      while credit_counter_in = 0 loop
        wait until clk'event and clk ='0';
      end loop;
 ---------------------------------------------------------------------

          wait until clk'event and clk ='0'; -- On negative edge of clk (for syncing purposes)

	          port_in <= Header_type &  std_logic_vector(to_unsigned(Packet_length, 12)) & std_logic_vector(to_unsigned(destination_id, 4)) &
                   std_logic_vector(to_unsigned(source_id, 4)) & "00000000" & XOR_REDUCE(Header_type &
                   std_logic_vector(to_unsigned(Packet_length, 12)) & std_logic_vector(to_unsigned(destination_id, 4)) &
                   std_logic_vector(to_unsigned(source_id, 4)& "00000000" ));

			  valid_out <= '1';
  --------------------------------------


            if credit_counter_in = "00" then
               valid_out <= '0';
               -- Wait until next router/NI has at least enough space for one flit in its input FIFO
               wait until credit_counter_in'event and credit_counter_in > 0;
               wait until clk'event and clk ='0';
            end if;
-------------------------------------------------------------------
			wait until clk'event and clk ='0';

			port_in <= Body_type &  std_logic_vector(to_unsigned(body_data, 28)) & XOR_REDUCE(Body_type & std_logic_vector(to_unsigned(body_data, 28)));

			valid_out <= '1';
			wait until clk'event and clk ='0';


  --------------------------------------
			 if credit_counter_in = "00" then
				 valid_out <= '0';
				 -- Wait until next router/NI has at least enough space for one flit in its input FIFO
				 wait until credit_counter_in'event and credit_counter_in > 0;
				 wait until clk'event and clk ='0';
			end if;
-----------------------------------------
			port_in <=  Tail_type &  std_logic_vector(to_unsigned(tail_data, 28)) & XOR_REDUCE(Tail_type & std_logic_vector(to_unsigned(tail_data, 28)));
			valid_out <= '1';
				wait until clk'event and clk ='0';
				valid_out <= '0';
				port_in <= "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ" ;
------------------------------------------
          if now > finish_time then
              wait;
          end if;
    end loop;
    wait;
  end gen_packet_from_file;

  procedure get_packet(DATA_WIDTH, initial_delay: in integer; --initial_delaywaits for this no. of clock cycles before sending the packet!
                       signal clk: in std_logic;
                       signal credit_out: out std_logic;
                       signal valid_in: in std_logic;
                       signal port_in: in std_logic_vector
                        ) is
			variable source_node, destination_node, P_length, packet_id, counter, body_data_read, tail_data_read: integer;
			variable LINEVARIABLE : line;
			file VEC_FILE : text ;

     begin
      file_open(VEC_FILE,"outputfile.txt",APPEND_MODE);
     credit_out <= '1';
     counter := 0;
     while true loop
         wait until clk'event and clk ='1';

         if valid_in = '1' then
              if (port_in(DATA_WIDTH-1 downto DATA_WIDTH-3) = "001") then
                counter := 1;
                P_length := to_integer(unsigned(port_in(28 downto 17)));
                destination_node := to_integer(unsigned(port_in(16 downto 13)));
                source_node := to_integer(unsigned(port_in(12 downto 9)));
                packet_id := to_integer(unsigned(port_in(8 downto 1)));
	            end if;

              if  (port_in(DATA_WIDTH-1 downto DATA_WIDTH-3) = "010")   then
        		      counter := counter+1;
        		      body_data_read := to_integer(unsigned(port_in(28 downto 1)));
              end if;

    		      if (port_in(DATA_WIDTH-1 downto DATA_WIDTH-3) = "100") then
                  counter := counter+1;
                 	tail_data_read := to_integer(unsigned(port_in(28 downto 1)));
    			        report "Packet received from" & integer'image(source_node) & " to " & integer'image(destination_node) & "with body:" & integer'image(body_data_read) & "with tail:" & integer'image(tail_data_read);
                  write(LINEVARIABLE, "Packet received from " & integer'image(source_node) & " to " & integer'image(destination_node) & " with body: "& integer'image(body_data_read) &  " with tail: " & integer'image(tail_data_read));
                  writeline(VEC_FILE, LINEVARIABLE);
                  counter := 0;
                  P_length:= 0;
                  destination_node:= 0;
                  source_node:= 0;
                  packet_id:= 0;
                  body_data_read:= 0;
                  tail_data_read:= 0;
              end if;


       end if;
     end loop;
  end get_packet;

end TB_Package;