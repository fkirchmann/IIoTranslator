/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.iiotranslator.device.drivers;

import com.iiotranslator.device.DeviceRequest;
import com.iiotranslator.device.DeviceRequestCompletionListener;
import com.iiotranslator.opc.FolderNode;
import com.iiotranslator.service.Device;

import java.util.List;

public interface DeviceDriver {
    /**
     * This method is called when the driver thread is started. The driver should create and expose the nodes it wishes
     * to represent within the given deviceFolderNode.
     *
     * @param device           The Device object containing the device's configuration.
     * @param deviceFolderNode The folder node in which the driver should create its nodes.
     */
    void initialize(Device device, FolderNode deviceFolderNode);

    /**
     * This method is called by the driver thread if there are any open read and/or write requests. The driver should
     * process the requests and complete them by calling the provided listener's completeWriteRequest or
     * completeReadRequest methods.
     *
     * @param requestQueue A list of open DeviceRequest objects for the driver to process.
     */
    void process(List<DeviceRequest> requestQueue, DeviceRequestCompletionListener listener);
}
