/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers.keyence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeyenceDriverCodes {

    @RequiredArgsConstructor
    @Getter
    public enum ErrorResponses {
        UNRECOGNIZABLE_ERROR(0, "Command Unrecognizable error", "An undefined identification code was sent.", "Check the data contents, and then send the correct data."),
        BUSY_ERROR(1, "Busy error", "Because the MK-U6000 is busy printing or performing another operation, the command cannot be executed.", "After printing or another command has finished, resend the command."),
        STATUS_ERROR(2, "Status error", "Because an abnormal or caution level error has occurred, the command cannot be executed.", "After confirming the error contents using the \"EW\" command and removing the cause of the error, cancel the error and change the MK-U6000 to RUN status."),
        PRIORITY_ERROR(3, "Priority error", "A setting or initialize command was sent while the console had priority.", "Switch the console to the Main screen, and then resend the command."),
        DATA_LENGTH_ERROR(20, "Data length error", "The data length of the command is invalid.", "Check the data contents, and then send the correct data."),
        DATA_RANGE_ERROR(22, "Data range error", "Data out of the setting range was received.", "Check the data contents, and then send the correct data."),
        MEMORY_OVER_ERROR(31, "Memory over error", "The command data exceeds the number of characters that can be stored in a single setting.", "Reduce the number of characters to 4,000 bytes or less, and then resend the command."),
        TIMEOUT_ERROR(40, "Time-out error", "The MK-U6000 is in a state such that it cannot process the response within 3 seconds.", "Resend the command (Ethernet only)."),
        INVALID_CHECKSUM_ERROR(90, "Invalid checksum error", "The checksum value is invalid.", "Check the data and checksum contents, and then send the correct data.");

        private final int code;
        private final String name, description, countermeasures;

        public String toString() {
            return String.format("ErrorResponse (code %d): %s / Description: %s / Countermeasures: %s", code, name, description, countermeasures);
        }
    }

    private static final Map<Integer, ErrorResponses> ERROR_RESPONSES;
    private static final Map<Integer, SystemStatusCodes> SYSTEM_STATUS_CODES;
    private static final Map<Integer, SystemErrorCodes> SYSTEM_ERROR_CODES;

    static {
        ERROR_RESPONSES = Collections.unmodifiableMap(Arrays.stream(ErrorResponses.values())
                .collect(HashMap::new, (m, v) -> m.put(v.getCode(), v), HashMap::putAll));
        SYSTEM_STATUS_CODES = Collections.unmodifiableMap(Arrays.stream(SystemStatusCodes.values())
                .collect(HashMap::new, (m, v) -> m.put(v.getCode(), v), HashMap::putAll));
        SYSTEM_ERROR_CODES = Collections.unmodifiableMap(Arrays.stream(SystemErrorCodes.values())
                .collect(HashMap::new, (m, v) -> m.put(v.getCode(), v), HashMap::putAll));
    }

    public static ErrorResponses getErrorResponse(int code) {
        return ERROR_RESPONSES.get(code);
    }

    public static SystemStatusCodes getSystemStatusCode(int code) {
        return SYSTEM_STATUS_CODES.get(code);
    }

    public static SystemErrorCodes getSystemErrorCode(int code) {
        return SYSTEM_ERROR_CODES.get(code);
    }

    @RequiredArgsConstructor
    @Getter
    public enum SystemStatusCodes {
        STOPPED(0, "Stopped"),
        PRINTABLE(1, "Printable"),
        STARTUP_COMPLETE_1(2, "Startup complete 1"),
        STARTUP_COMPLETE_2(3, "Startup complete 2"),
        SUSPENDED(4, "Suspended"),
        STARTING(5, "Starting"),
        SHUTTING_DOWN(6, "Shutting Down"),
        CHARGE_ADJUSTING(7, "Charge adjusting"),
        INK_PARTICLE_ADJUSTING(8, "Ink particle adjusting"),
        CLEANING_BEFORE_LONG_TERM_STORAGE(9, "Cleaning before long term storage"),
        PATH_RECOVERING(10, "Path recovering"),
        PAUSED(11, "Paused"),
        EMERGENCY_STOPPING(12, "Emergency Stopping"),
        COLLECTING_INK_1(13, "Collecting Ink 1"),
        NOZZLE_SUCKING(14, "Nozzle sucking"),
        GUTTER_SUCKING(15, "Gutter sucking"),
        INTERMITTENT_INJECTING_SOLV(16, "Intermittent injecting(Solv)"),
        AXIS_ADJUSTING_SOLV_NOZZLE_REPLACING(17, "Axis adjusting (Solv) / Nozzle Replacing"),
        AXIS_ADJUSTING_INK_PRESSURE_ADJUSTING(18, "Axis adjusting (Ink) / Pressure adjusting"),
        MAIN_TANK_DRAINING(19, "Main tank draining"),
        CONDITIONING_TANK_DRAINING(20, "Conditioning tank draining"),
        DRAINING_ALL(21, "Draining all"),
        AUTO_SHOWER_EXECUTING(22, "Auto-shower executing"),
        AUTO_SHOWER_STRONG_EXECUTING(23, "Auto-shower (strong) executing"),
        AUTOMATIC_CIRCULATION_IN_OPERATION(24, "Automatic circulation in operation"),
        PRE_STORAGE_CLEANING_INTERNAL_DRYING(25, "Pre storage cleaning (internal drying)"),
        SLEEP_MODE_STARTUP(26, "Sleep mode (startup)"),
        PRE_STORAGE_CLEANING(27, "Pre storage cleaning"),
        FILTER_A_REPLACING_PREPARATION(28, "Filter A Replacing (Preparation)"),
        FILTER_B_REPLACING_PREPARATION(29, "Filter B Replacing (Preparation)"),
        REPLACING_PUMP_PREPARATION(30, "Replacing pump (Preparation)"),
        PATH_RECOVERING_POST(31, "Path recovering (Post)"),
        SLEEP_MODE_SHUTDOWN(32, "Sleep mode (shutdown)"),
        SLEEP_MODE_WAITING(33, "Sleep mode (waiting)"),
        SLEEP_MODE_OPERATING(34, "Sleep mode (operating)"),
        STOPPED_VIEWING_CLEANING_AND_SHUTDOWN_CONFIRMING_SCREEN(35, "Stopped (viewing cleaning and shutdown confirming screen)"),
        STOPPED_VIEWING_AUTOMATIC_CIRCULATION_CONFIRMING_SCREEN(36, "Stopped (viewing automatic circulation confirming screen)(PY/PW)"),
        CANCEL_MAINTENANCE(37, "Cancel maintenance"),
        RESTART_MAINTENANCE(38, "Restart Maintenance"),
        PAUSING_MAINTENANCE(39, "Pausing Maintenance"),
        FILTER_A_REPLACING_CONFIRMING_REMAINING(40, "Filter A Replacing (Confirming Remaining)"),
        FILTER_A_REPLACING_POST(41, "Filter A Replacing (Post)"),
        REPLACING_PUMP_POST(42, "Replacing pump (Post)"),
        STOPPED_REPLACING(43, "Stopped (Replacing)"),
        COLLECTING_INK_2(44, "Collecting Ink 2"),
        PRE_STORAGE_CLEANING_PUMP_DRYING(45, "Pre storage cleaning (pump drying)"),
        AUTO_SHOWER_EXECUTING_DRYING(46, "Auto-shower executing (Drying)"),
        NOZZLE_CLEANING(47, "Nozzle cleaning"),
        PRE_SLEEP_CLEANING(48, "Pre sleep cleaning"),
        INK_PATH_UNIT_REPLACING_PREPARATION(81, "Ink Path Unit Replacing (Preparation)"),
        INK_PATH_UNIT_REPLACING_POST(82, "Ink Path Unit Replacing (Post)");

        private final int code;
        private final String name;

        public String toString() {
            return String.format("SystemStatusCode (code %d): %s", code, name);
        }
    }

    /**
     * Ordered from least to most severe.
     */
    public enum ErrorLevel implements Comparable<ErrorLevel> {
        OK, CAUTION, ABNORMAL
    }

    @RequiredArgsConstructor
    @Getter
    public enum SystemErrorCodes {
        MAIN_TANK_EMPTY_ERROR(1, ErrorLevel.ABNORMAL, "Main Tank Empty Error"),
        MAIN_TANK_FULL_ERROR(2, ErrorLevel.ABNORMAL, "Main Tank Full Error"),
        CONDITIONING_TANK_FULL_ERROR(3, ErrorLevel.ABNORMAL, "Conditioning Tank Full Error"),
        MAIN_TANK_LEVEL_GAUGE_ERROR(4, ErrorLevel.ABNORMAL, "Main Tank Level Gauge Error"),
        CONDITIONING_TANK_LEVEL_GAUGE_ERROR(5, ErrorLevel.ABNORMAL, "Conditioning Tank Level Gauge Error"),
        VISCOMETER_LEVEL_ERROR(6, ErrorLevel.ABNORMAL, "Viscometer Level Error"),
        INK_PRESSURE_LOW_ERROR(7, ErrorLevel.ABNORMAL, "Ink Pressure (Low) Error"),
        INK_PRESSURE_HIGH_ERROR(8, ErrorLevel.ABNORMAL, "Ink Pressure (High) Error"),
        CONTROLLER_TEMP_HIGH_ERROR(9, ErrorLevel.ABNORMAL, "Controller Temp High Error"),
        CONTROLLER_TEMP_LOW_ERROR(10, ErrorLevel.ABNORMAL, "Controller Temp Low Error"),
        HEAD_TEMP_HIGH_ERROR(11, ErrorLevel.ABNORMAL, "Head Temp High Error"),
        HEAD_TEMP_LOW_ERROR(12, ErrorLevel.ABNORMAL, "Head Temp Low Error"),
        TRIGGER_ON_TIME_OVER_ERROR(13, ErrorLevel.ABNORMAL, "Trigger On Time Over Error"),
        VOLTAGE_LEAK_ERROR(14, ErrorLevel.ABNORMAL, "Voltage Leak Error"),
        HEAD_COVER_OPEN_ERROR(15, ErrorLevel.ABNORMAL, "Head Cover Open Error"),
        GUTTER_SENSOR_ERROR_1(16, ErrorLevel.ABNORMAL, "Gutter Sensor Error 1"),
        NOZZLE_CLOGGING_ERROR(17, ErrorLevel.ABNORMAL, "Nozzle Clogging Error"),
        ENCODER_SPEED_OVER_ERROR(18, ErrorLevel.ABNORMAL, "Encoder Speed Over Error"),
        PRINT_TRIGGER_OVERLAP_ERROR(19, ErrorLevel.ABNORMAL, "Print Trigger Overlap Error"),
        SHORT_PRINT_INTERVAL_ERROR(20, ErrorLevel.ABNORMAL, "Short Print Interval Error"),
        TRACKING_COUNT_OVER_ERROR(21, ErrorLevel.ABNORMAL, "Tracking Count Over Error"),
        SHORT_TRIGGER_DELAY_ERROR(22, ErrorLevel.ABNORMAL, "Short Trigger Delay Error"),
        SHORT_CONTINUOUS_PRINT_TRIGGER_ERROR(23, ErrorLevel.ABNORMAL, "Short Continuous Print Trigger Error"),
        SYSTEM_MEMORY_ERROR(24, ErrorLevel.ABNORMAL, "System Memory Error"),
        PUMP_LIFESPAN_ERROR(25, ErrorLevel.ABNORMAL, "Pump Lifespan Error"),
        PUMP_CONTROL_ERROR(26, ErrorLevel.ABNORMAL, "Pump Control Error"),
        REPLACING_PUMP(27, ErrorLevel.ABNORMAL, "Replacing Pump"),
        HARD_ERROR_1(28, ErrorLevel.ABNORMAL, "Hard Error 1"),
        HARD_ERROR_2(29, ErrorLevel.ABNORMAL, "Hard Error 2"),
        HARD_ERROR_3(30, ErrorLevel.ABNORMAL, "Hard Error 3"),
        SOFT_ERROR_1(31, ErrorLevel.ABNORMAL, "Soft Error 1"),
        FILTER_A_LIFESPAN_ERROR(32, ErrorLevel.ABNORMAL, "Filter A Lifespan Error"),
        PHASE_ALIGNMENT_ERROR_5_MINUTES_ELAPSED(33, ErrorLevel.ABNORMAL, "Phase Alignment Error (5 Minutes Elapsed)"),
        FILTER_B_LIFESPAN_ERROR(34, ErrorLevel.ABNORMAL, "Filter B Lifespan Error"),
        AIR_INTAKE_FAN_LOCKED_ERROR(35, ErrorLevel.ABNORMAL, "Air Intake Fan Locked Error"),
        INTERNAL_FAN_LOCKED_ERROR(36, ErrorLevel.ABNORMAL, "Internal Fan Locked Error"),
        HEATER_CONTROL_ERROR(37, ErrorLevel.ABNORMAL, "Heater Control Error"),
        PIEZO_FB_ERROR(38, ErrorLevel.ABNORMAL, "Piezo FB Error"),
        DIRTY_CHARGING_SENSOR_ERROR(39, ErrorLevel.ABNORMAL, "Dirty Charging Sensor Error"),
        PHASE_ALIGNMENT_ERROR(40, ErrorLevel.ABNORMAL, "Phase Alignment Error"),
        RS232C_COMMUNICATION_ERROR(41, ErrorLevel.ABNORMAL, "RS232C Communication Error"),
        FRONT_DATA_ERROR(42, ErrorLevel.ABNORMAL, "Front Data Error"),
        PRINTING_STOP_INPUT_ON_ERROR(43, ErrorLevel.ABNORMAL, "Printing Stop Input ON (Terminal)"),
        INK_PARTICLES_FORMING(44, ErrorLevel.ABNORMAL, "Ink Particles Forming"),
        GUTTER_SENSOR_ERROR_2(45, ErrorLevel.ABNORMAL, "Gutter Sensor Error 2"),
        CANNOT_ADJUST_VISCOSITY_ERROR(46, ErrorLevel.ABNORMAL, "Cannot Adjust Viscosity Error"),
        CARTRIDGE_HOLDER_OPEN_ERROR(47, ErrorLevel.ABNORMAL, "Cartridge Holder Open Error"),
        INTERNAL_COMMUNICATION_ERROR(48, ErrorLevel.ABNORMAL, "Internal Communication Error"),
        DIRT_IN_PRINT_HEAD_ERROR(49, ErrorLevel.ABNORMAL, "Dirt In Print Head Error"),
        INSUFFICIENT_CHARGING_DATA_ERROR(50, ErrorLevel.ABNORMAL, "Insufficient Charging Data Error"),
        CHARGING_OFFSET_ERROR(51, ErrorLevel.ABNORMAL, "Charging Offset Error"),
        SOLENOID_VALVE_NO_6_ERROR(52, ErrorLevel.ABNORMAL, "Solenoid Valve No. 6 Error"),
        SOLENOID_VALVE_NO_12_ERROR(53, ErrorLevel.ABNORMAL, "Solenoid Valve No. 12 Error"),
        SOLENOID_VALVE_NO_14_ERROR(54, ErrorLevel.ABNORMAL, "Solenoid Valve No. 14 Error"),
        SOLENOID_VALVE_NO_15_ERROR(55, ErrorLevel.ABNORMAL, "Solenoid Valve No. 15 Error"),
        HEATER_ERROR(56, ErrorLevel.ABNORMAL, "Heater Error"),
        AIR_PUMP_ERROR(57, ErrorLevel.ABNORMAL, "Air Pump Error"),
        DEFLECTOR_VOLTAGE_LEAK_ERROR(58, ErrorLevel.ABNORMAL, "Deflector Voltage Leak Error (5 Minutes Elapsed)"),
        HEAD_COVER_OPEN_ERROR_2(59, ErrorLevel.ABNORMAL, "Head Cover Open Error 2"),
        INK_PATH_UNIT_LIFESPAN_ERROR(60, ErrorLevel.ABNORMAL, "Ink Path Unit Lifespan Error"),
        MAINTENANCE_NOT_COMPLETE_ERROR(64, ErrorLevel.ABNORMAL, "Maitenance Not Complete Error"),
        MAIN_TANK_SUPPLY_ABNORMAL_ERROR(66, ErrorLevel.ABNORMAL, "Main Tank Supply Abnormal Error"),
        DRAIN_ERROR(67, ErrorLevel.ABNORMAL, "Drain Error"),
        PRESSURE_ERROR(68, ErrorLevel.ABNORMAL, "Pressure Error"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_ERROR_1(69, ErrorLevel.ABNORMAL, "CV-X Send Judgment Character String Error 1"),
        CV_X_TRIGGER_DELAY_ERROR(70, ErrorLevel.ABNORMAL, "CV-X Trigger Delay Error"),
        CV_X_TRIGGER_TRACKING_COUNT_OVER_ERROR(71, ErrorLevel.ABNORMAL, "CV-X Trigger Tracking Count Over Error"),
        TRIGGER_DELAY_OVER_ERROR(72, ErrorLevel.ABNORMAL, "Trigger Delay Over Error"),
        VISCOMETER_COUNT_ERROR_1(73, ErrorLevel.ABNORMAL, "Viscometer Count Error 1"),
        VISCOMETER_COUNT_ERROR_2(74, ErrorLevel.ABNORMAL, "Viscometer Count Error 2"),
        VISCOMETER_COUNT_ERROR_3(75, ErrorLevel.ABNORMAL, "Viscometer Count Error 3"),
        VISCOMETER_COUNT_ERROR_4(76, ErrorLevel.ABNORMAL, "Viscometer Count Error 4"),
        ETHERNET_COMMUNICATION_DISCONNECT_ERROR(91, ErrorLevel.ABNORMAL, "Ethernet Communication Disconnect Error"),
        CV_X_LINKED_COMMUNICATION_DISCONNECT_ERROR(92, ErrorLevel.ABNORMAL, "CV-X Linked Communication Disconnect Error"),
        INTERNAL_COMMUNICATION_TIMEOUT_ERROR(93, ErrorLevel.ABNORMAL, "Internal Communication Timeout Error"),
        SOFT_ERROR_2(94, ErrorLevel.ABNORMAL, "Soft Error 2"),
        SNTP_TIME_MISALIGNMENT_DETECTED(95, ErrorLevel.ABNORMAL, "SNTP Time Misalignment Detected"),
        CV_X_INSPECTION_SETTING_SWITCH_ERROR_1(96, ErrorLevel.ABNORMAL, "CV-X Inspection Setting Switch Error 1"),
        CV_X_INSPECTION_SETTING_SWITCH_ERROR_2(97, ErrorLevel.ABNORMAL, "CV-X Inspection Setting Switch Error 2"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_ERROR_2(98, ErrorLevel.ABNORMAL, "CV-X Send Judgment Character String Error 2"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_ERROR_3(99, ErrorLevel.ABNORMAL, "CV-X Send Judgment Character String Error 3"),

        INK_CARTRIDGE_EMPTY_WARNING(101, ErrorLevel.CAUTION, "Ink Cartridge Empty Warning"),
        SOLVENT_CARTRIDGE_EMPTY_WARNING(102, ErrorLevel.CAUTION, "Solvent Cartridge Empty Warning"),
        CARTRIDGE_HOLDER_OPEN(103, ErrorLevel.CAUTION, "Cartridge Holder Open"),
        INK_VISCOSITY_THICK_WARNING(104, ErrorLevel.CAUTION, "Ink Viscosity (Thick) Warning"),
        INK_VISCOSITY_THIN_WARNING(105, ErrorLevel.CAUTION, "Ink Viscosity (Thin) Warning"),
        CONTROLLER_TEMPERATURE_HIGH_WARNING(106, ErrorLevel.CAUTION, "Controller Temperature High Warning"),
        CONTROLLER_TEMPERATURE_LOW_WARNING(107, ErrorLevel.CAUTION, "Controller Temperature Low Warning"),
        HEAD_TEMPERATURE_HIGH_WARNING(108, ErrorLevel.CAUTION, "Head Temperature High Warning"),
        HEAD_TEMPERATURE_LOW_WARNING(109, ErrorLevel.CAUTION, "Head Temperature Low Warning"),
        VISCOMETER_COUNT_WARNING_1(110, ErrorLevel.CAUTION, "Viscometer Count Warning 1"),
        VISCOMETER_COUNT_WARNING_2(111, ErrorLevel.CAUTION, "Viscometer Count Warning 2"),
        VISCOMETER_COUNT_WARNING_3(112, ErrorLevel.CAUTION, "Viscometer Count Warning 3"),
        VISCOMETER_COUNT_WARNING_4(113, ErrorLevel.CAUTION, "Viscometer Count Warning 4"),
        FILTER_A_REPLACEMENT_WARNING(114, ErrorLevel.CAUTION, "Filter A Replacement Warning"),
        FILTER_B_REPLACEMENT_WARNING(116, ErrorLevel.CAUTION, "Filter B Replacement Warning"),
        PUMP_LIFESPAN_WARNING(117, ErrorLevel.CAUTION, "Pump Lifespan Warning"),
        PUMP_REPLACEMENT_WARNING(118, ErrorLevel.CAUTION, "Pump Replacement Warning"),
        SHORT_CONTINUOUS_PRINT_TRIGGER_WARNING(119, ErrorLevel.CAUTION, "Short Continuous Print Trigger Warning"),
        ENCODER_SPEED_OVER_WARNING(120, ErrorLevel.CAUTION, "Encoder Speed Over Warning"),
        PRINTING_STOP_ON_TERMINAL(121, ErrorLevel.CAUTION, "Printing Stop ON (Terminal)"),
        SNTP_TIME_MISALIGNMENT_DETECTED_2(122, ErrorLevel.CAUTION, "SNTP Time Misalignment Detected"),
        TRACKING_COUNT_OVER_WARNING(123, ErrorLevel.CAUTION, "Tracking Count Over Warning"),
        ETHERNET_COMMUNICATION_DISCONNECT_WARNING(124, ErrorLevel.CAUTION, "Ethernet Communication Disconnect Warning"),
        CV_X_LINKED_COMMUNICATION_DISCONNECT_WARNING(125, ErrorLevel.CAUTION, "CV-X Linked Communication Disconnect Warning"),
        PRINT_TRIGGER_DETECTED_DURING_READY_OFF(126, ErrorLevel.CAUTION, "Print Trigger Detected During Ready OFF"),
        INK_PATH_UNIT_LIFESPAN_WARNING(127, ErrorLevel.CAUTION, "Ink Path Unit Lifespan Warning"),
        SOLENOID_VALVE_SIMULTANEOUS_DRIVE(128, ErrorLevel.CAUTION, "Solenoid Valve Simultaneous Drive"),
        FILTER_A_LIFESPAN_WARNING(129, ErrorLevel.CAUTION, "Filter A Lifespan Warning"),
        FILTER_B_LIFESPAN_WARNING(131, ErrorLevel.CAUTION, "Filter B Lifespan Warning"),
        INK_CARTRIDGE_UNRECOGNIZABLE(132, ErrorLevel.CAUTION, "Ink Cartridge Unrecognizable"),
        SOLVENT_CARTRIDGE_UNRECOGNIZABLE(133, ErrorLevel.CAUTION, "Solvent Cartridge Unrecognizable"),
        INK_CARTRIDGE_NOT_INSERTED(134, ErrorLevel.CAUTION, "Ink Cartridge Not Inserted"),
        SOLVENT_CARTRIDGE_NOT_INSERTED(135, ErrorLevel.CAUTION, "Solvent Cartridge Not Inserted"),
        INK_CARTRIDGE_TYPE_MISMATCH(136, ErrorLevel.CAUTION, "Ink Cartridge Type Mismatch"),
        SOLVENT_CARTRIDGE_TYPE_MISMATCH(137, ErrorLevel.CAUTION, "Solvent Cartridge Type Mismatch"),
        INK_CARTRIDGE_EXPIRATION_DATE_SOON(138, ErrorLevel.CAUTION, "Ink Cartridge Expiration Date Soon"),
        SOLVENT_CARTRIDGE_EXPIRATION_DATE_SOON(139, ErrorLevel.CAUTION, "Solvent Cartridge Expiration Date Soon"),
        LOW_INK_CARTRIDGE_LEVEL(140, ErrorLevel.CAUTION, "Low Ink Cartridge Level"),
        LOW_SOLVENT_CARTRIDGE_LEVEL(141, ErrorLevel.CAUTION, "Low Solvent Cartridge Level"),
        CONDITIONING_TANK_FULL_WARNING(142, ErrorLevel.CAUTION, "Conditioning Tank Full Warning"),
        LONG_TERM_STOP_DETECTED(143, ErrorLevel.CAUTION, "Long-term Stop Detected"),
        ILLEGAL_POWER_DISCONNECT_DETECTED(145, ErrorLevel.CAUTION, "Illegal Power Disconnect Detected"),
        INK_CARTRIDGE_EXPIRED(147, ErrorLevel.CAUTION, "Ink Cartridge Expired"),
        SOLVENT_CARTRIDGE_EXPIRED(148, ErrorLevel.CAUTION, "Solvent Cartridge Expired"),
        INK_CARTRIDGE_NOT_INSERTED_FOR_1_HOUR_OR_MORE(149, ErrorLevel.CAUTION, "Ink Cartridge Not Inserted for 1 Hour or More"),
        SOLVENT_CARTRIDGE_NOT_INSERTED_FOR_1_HOUR_OR_MORE(150, ErrorLevel.CAUTION, "Solvent Cartridge Not Inserted for 1 Hour or More"),
        CLEANING_INSIDE_PATH_REQUIRED(151, ErrorLevel.CAUTION, "Cleaning Inside Path Required"),
        RS_232_COMMUNICATION_ERROR(152, ErrorLevel.CAUTION, "RS-232 Communication Error"),
        AIR_INTAKE_FAN_LOCKED_WARNING(153, ErrorLevel.CAUTION, "Air Intake Fan Locked Warning"),
        INTERNAL_FAN_LOCKED_WARNING(154, ErrorLevel.CAUTION, "Internal Fan Locked Warning"),
        CV_X_TRIGGER_TRACKING_COUNT_OVER_WARNING(155, ErrorLevel.CAUTION, "CV-X Trigger Tracking Count Over Warning"),
        PUMP_PRIMING_NOT_COMPLETE(156, ErrorLevel.CAUTION, "Pump priming not complete"),
        AIR_PUMP_ERROR_2(157, ErrorLevel.CAUTION, "Air Pump Error"),
        INK_PATH_UNIT_REPLACEMENT_WARNING(158, ErrorLevel.CAUTION, "Ink Path Unit Replacement Warning"),
        MAIN_TANK_FULL_WARNING(159, ErrorLevel.CAUTION, "Main Tank Full Warning"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_WARNING_1(160, ErrorLevel.CAUTION, "CV-X Send Judgment Character String Warning 1"),
        CV_X_INSPECTION_SETTING_SWITCH_WARNING_1(181, ErrorLevel.CAUTION, "CV-X Inspection Setting Switch Warning 1"),
        CV_X_INSPECTION_SETTING_SWITCH_WARNING_2(182, ErrorLevel.CAUTION, "CV-X Inspection Setting Switch Warning 2"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_WARNING_2(183, ErrorLevel.CAUTION, "CV-X Send Judgment Character String Warning 2"),
        CV_X_SEND_JUDGMENT_CHARACTER_STRING_WARNING_3(184, ErrorLevel.CAUTION, "CV-X Send Judgment Character String Warning 3"),
        LOW_MAIN_TANK_LEVEL(185, ErrorLevel.CAUTION, "Low Main Tank Level"),
        LEFT_SOLVENT_CARTRIDGE_EMPTY_WARNING(186, ErrorLevel.CAUTION, "Left Solvent Cartridge Empty Warning"),
        INK_CARTRIDGE_SETTLING_WARNING(187, ErrorLevel.CAUTION, "Ink Cartridge Settling Warning");

        private final int code;
        private final ErrorLevel level;
        private final String name;
    }
}
