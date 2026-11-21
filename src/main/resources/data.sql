MERGE INTO products (mpn, item_name, manufacturer, price, currency, quantity, status, version, category, package_type, specifications) KEY(mpn)
VALUES
-- Operational amplifiers
('LM358',    'Operational Amplifier',                 'Texas Instruments',  25.50, 'INR', 1000, 'ACTIVE',   0, 'Operational Amplifier', 'DIP-8',  'Dual channel, GBW 1 MHz, supply 3-32 V, low power'),
('LM358-ST', 'Operational Amplifier',                 'STMicroelectronics', 22.75, 'INR', 1500, 'ACTIVE',   0, 'Operational Amplifier', 'SOIC-8', 'Dual channel, GBW 1 MHz, supply 3-32 V, low power'),
('LM358-ON', 'Operational Amplifier',                 'ON Semiconductor',   24.00, 'INR',  800, 'ACTIVE',   0, 'Operational Amplifier', 'DIP-8',  'Dual channel, GBW 1 MHz, supply 3-32 V, low power'),
('LM358B',   'Operational Amplifier',                 'Texas Instruments',   0.45, 'USD',  700, 'ACTIVE',   0, 'Operational Amplifier', 'SOIC-8', 'Dual channel, GBW 1.2 MHz, supply 3-36 V, low power'),
('MC1458',   'Dual Operational Amplifier',            'ON Semiconductor',   28.00, 'INR',    0, 'ACTIVE',   0, 'Operational Amplifier', 'DIP-8',  'Dual channel, GBW 1 MHz, supply 10-36 V'),
('LM741',    'Operational Amplifier',                 'Texas Instruments',  20.00, 'INR',  300, 'INACTIVE', 0, 'Operational Amplifier', 'DIP-8',  'Single channel, GBW 1 MHz, supply 10-36 V'),
('TL072',    'Low Noise Dual Operational Amplifier',  'Texas Instruments',  32.00, 'INR',  600, 'ACTIVE',   0, 'Operational Amplifier', 'DIP-8',  'Dual channel JFET input, GBW 3 MHz, supply 4.5-40 V, low noise'),
('LM324',    'Quad Operational Amplifier',            'Texas Instruments',  27.00, 'INR',  900, 'ACTIVE',   0, 'Operational Amplifier', 'DIP-14', 'Quad channel, GBW 1.2 MHz, supply 3-32 V'),
-- Timers
('NE555',    'Timer IC',                              'Texas Instruments',  18.00, 'INR',  500, 'ACTIVE',   0, 'Timer IC', 'DIP-8',  'Single timer, supply 4.5-16 V, 200 mA output'),
('NE555-ST', 'Timer IC',                              'STMicroelectronics', 16.50, 'INR', 1200, 'ACTIVE',   0, 'Timer IC', 'SOIC-8', 'Single timer, supply 4.5-16 V, 200 mA output'),
('LMC555',   'CMOS Timer IC',                         'Texas Instruments',  35.00, 'INR',  300, 'ACTIVE',   0, 'Timer IC', 'SOIC-8', 'Single CMOS timer, supply 1.5-15 V, low power'),
('NE556',    'Dual Timer IC',                         'Texas Instruments',  30.00, 'INR',  400, 'ACTIVE',   0, 'Timer IC', 'DIP-14', 'Dual timer, supply 4.5-16 V'),
-- Voltage regulators
('LM7805',      '5V Voltage Regulator',                   'Texas Instruments',            12.00, 'INR', 2000, 'ACTIVE', 0, 'Voltage Regulator', 'TO-220',  'Fixed 5 V output, 1.5 A, input up to 35 V'),
('L7805',       '5V Voltage Regulator',                   'STMicroelectronics',           10.50, 'INR', 2500, 'ACTIVE', 0, 'Voltage Regulator', 'TO-220',  'Fixed 5 V output, 1.5 A, input up to 35 V'),
('LM7812',      '12V Voltage Regulator',                  'Texas Instruments',            13.00, 'INR', 1500, 'ACTIVE', 0, 'Voltage Regulator', 'TO-220',  'Fixed 12 V output, 1.5 A, input up to 35 V'),
('AMS1117-3.3', '3.3V Low Dropout Voltage Regulator',     'Advanced Monolithic Systems',   8.00, 'INR', 5000, 'ACTIVE', 0, 'Voltage Regulator', 'SOT-223', 'Fixed 3.3 V output, 1 A, low dropout'),
-- Transistors
('2N2222A', 'NPN Transistor', 'ON Semiconductor', 4.50, 'INR', 10000, 'ACTIVE', 0, 'Transistor', 'TO-18', 'NPN, 40 V, 800 mA'),
('BC547',   'NPN Transistor', 'ON Semiconductor', 3.00, 'INR',  8000, 'ACTIVE', 0, 'Transistor', 'TO-92', 'NPN, 45 V, 100 mA'),
-- Microcontrollers and modules
('ATMEGA328P-PU',  '8-bit Microcontroller',         'Microchip Technology', 210.00, 'INR', 150, 'ACTIVE', 0, 'Microcontroller',  'DIP-28',     '8-bit AVR, 32 KB flash, 20 MHz'),
('STM32F103C8T6',  'ARM Cortex-M3 Microcontroller', 'STMicroelectronics',   320.00, 'INR',  90, 'ACTIVE', 0, 'Microcontroller',  'LQFP-48',    '32-bit ARM Cortex-M3, 64 KB flash, 72 MHz'),
('ESP32-WROOM-32', 'WiFi Bluetooth Module',         'Espressif Systems',    285.00, 'INR',   0, 'ACTIVE', 0, 'Wireless Module', 'SMD Module', 'WiFi and Bluetooth, dual core 240 MHz, 4 MB flash'),
-- Passives
('RC0402FR-0710KL',    'Resistor 10k ohm 0402 1%',     'Yageo',  0.40, 'INR', 50000, 'ACTIVE', 0, 'Resistor',  '0402', '10 kOhm, 1% tolerance, 0.063 W'),
('CRCW040210K0FKED',   'Resistor 10k ohm 0402 1%',     'Vishay', 0.55, 'INR', 30000, 'ACTIVE', 0, 'Resistor',  '0402', '10 kOhm, 1% tolerance, 0.063 W'),
('GRM155R71C104KA88D', 'Ceramic Capacitor 100nF 0402', 'Murata', 0.90, 'INR', 40000, 'ACTIVE', 0, 'Capacitor', '0402', '100 nF, X7R, 16 V');
