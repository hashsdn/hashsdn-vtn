/*
 * Copyright (c) 2012-2013 NEC Corporation
 * All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vtn.javaapi.validation.logical;

import com.google.gson.JsonObject;
import org.opendaylight.vtn.core.util.Logger;
import org.opendaylight.vtn.javaapi.constants.VtnServiceConsts;
import org.opendaylight.vtn.javaapi.constants.VtnServiceJsonConsts;
import org.opendaylight.vtn.javaapi.exception.VtnServiceException;
import org.opendaylight.vtn.javaapi.ipc.enums.UncJavaAPIErrorCode;
import org.opendaylight.vtn.javaapi.resources.AbstractResource;
import org.opendaylight.vtn.javaapi.resources.logical.PortMapResource;
import org.opendaylight.vtn.javaapi.validation.CommonValidator;
import org.opendaylight.vtn.javaapi.validation.VtnServiceValidator;

/**
 * The Class PortMapResourceValidator validates request Json object for PortMap
 * API.
 */
public class PortMapResourceValidator extends VtnServiceValidator {

	private static final Logger LOG = Logger
			.getLogger(PortMapResourceValidator.class.getName());

	private final AbstractResource resource;
	final CommonValidator validator = new CommonValidator();

	/**
	 * Instantiates a new port map resource validator.
	 * 
	 * @param resource
	 *            the instance of AbstractResource
	 */
	public PortMapResourceValidator(final AbstractResource resource) {
		this.resource = resource;
	}

	/**
	 * Validate uri parameters for PortMap API.
	 * 
	 * @return true, if successful
	 */
	@Override
	public boolean validateUri() {
		LOG.trace("Start PortMapResourceValidator#validateUri()");
		boolean isValid = false;
		setInvalidParameter(VtnServiceJsonConsts.URI
				+ VtnServiceJsonConsts.VTNNAME);
		if (resource instanceof PortMapResource
				&& ((PortMapResource) resource).getVtnName() != null
				&& !((PortMapResource) resource).getVtnName().trim().isEmpty()) {
			isValid = validator.isValidMaxLengthAlphaNum(
					((PortMapResource) resource).getVtnName().trim(),
					VtnServiceJsonConsts.LEN_31);
			if (isValid) {
				setInvalidParameter(VtnServiceJsonConsts.URI
						+ VtnServiceJsonConsts.VBRNAME);
				if (((PortMapResource) resource).getVbrName() != null
						&& !((PortMapResource) resource).getVbrName().trim()
								.isEmpty()) {
					isValid = validator.isValidMaxLengthAlphaNum(
							((PortMapResource) resource).getVbrName().trim(),
							VtnServiceJsonConsts.LEN_31);
				} else {
					isValid = false;
				}
			}
			if (isValid) {
				setInvalidParameter(VtnServiceJsonConsts.URI
						+ VtnServiceJsonConsts.IFNAME);
				if (((PortMapResource) resource).getIfName() != null
						&& !((PortMapResource) resource).getIfName().trim()
								.isEmpty()) {
					isValid = validator.isValidMaxLengthAlphaNum(
							((PortMapResource) resource).getIfName().trim(),
							VtnServiceJsonConsts.LEN_31);
				} else {
					isValid = false;
				}
			}
			setListOpFlag(false);
		}
		LOG.trace("Completed PortMapResourceValidator#validateUri()");
		return isValid;
	}

	/**
	 * Validate request Json object for get, put method of PortMap API.
	 */
	@Override
	public void validate(final String method, final JsonObject requestBody)
			throws VtnServiceException {
		LOG.trace("Start PortMapResourceValidator#validate()");
		LOG.info("Validating request for " + method
				+ " of PortMapResourceValidator");
		boolean isValid = false;
		try {
			isValid = validateUri();
			if (isValid && requestBody != null
					&& VtnServiceConsts.GET.equals(method)) {
				isValid = validator.isValidGet(requestBody, isListOpFlag());
				setInvalidParameter(validator.getInvalidParameter());
				updateOpParameterForList(requestBody);
			} else if (isValid && requestBody != null
					&& VtnServiceConsts.PUT.equals(method)) {
				isValid = validatePut(requestBody);
			} else {
				isValid = false;
			}
		} catch (final NumberFormatException e) {
			LOG.error("Inside catch:NumberFormatException");
			if (method.equals(VtnServiceConsts.GET)) {
				setInvalidParameter(validator.getInvalidParameter());
			}
			isValid = false;
		} catch (final ClassCastException e) {
			if (method.equals(VtnServiceConsts.GET)) {
				setInvalidParameter(validator.getInvalidParameter());
			}
			LOG.error("Inside catch:ClassCastException");
			isValid = false;
		}
		// Throws exception if validation fails
		if (!isValid) {
			LOG.error("Validation failed");
			throw new VtnServiceException(Thread.currentThread()
					.getStackTrace()[1].getMethodName(),
					UncJavaAPIErrorCode.VALIDATION_ERROR.getErrorCode(),
					UncJavaAPIErrorCode.VALIDATION_ERROR.getErrorMessage());
		}
		LOG.info("Validation successful");
		LOG.trace("Complete PortMapResourceValidator#validate()");

	}

	/**
	 * validate put request Json object for PortMap API.
	 * 
	 * @param requestBody
	 *            the request Json object
	 * @return true, if successful
	 */
	private boolean validatePut(final JsonObject requestBody) {
		LOG.trace("Start PortMapResourceValidator#validatePut()");
		boolean isValid = false;
		setInvalidParameter(VtnServiceJsonConsts.PORTMAP);
		if (requestBody.has(VtnServiceJsonConsts.PORTMAP)
				&& requestBody.get(VtnServiceJsonConsts.PORTMAP).isJsonObject()) {
			final JsonObject portMap = requestBody
					.getAsJsonObject(VtnServiceJsonConsts.PORTMAP);
			// validation for key: switch_id(mandatory)
			setInvalidParameter(VtnServiceJsonConsts.LOGICAL_PORT_ID);
			if (portMap.has(VtnServiceJsonConsts.LOGICAL_PORT_ID)
					&& portMap.getAsJsonPrimitive(
							VtnServiceJsonConsts.LOGICAL_PORT_ID).getAsString() != null
					&& !portMap
							.getAsJsonPrimitive(
									VtnServiceJsonConsts.LOGICAL_PORT_ID)
							.getAsString().trim().isEmpty()) {
				isValid = validator.isValidMaxLength(
						portMap.getAsJsonPrimitive(
								VtnServiceJsonConsts.LOGICAL_PORT_ID)
								.getAsString().trim(),
						VtnServiceJsonConsts.LEN_255);
			}
			if (isValid) {
				// validation for key: vlan_id
				setInvalidParameter(VtnServiceJsonConsts.VLANID);
				if (portMap.has(VtnServiceJsonConsts.VLANID)
						&& portMap.getAsJsonPrimitive(
								VtnServiceJsonConsts.VLANID).getAsString() != null) {
					isValid = validator.isValidRange(portMap
							.getAsJsonPrimitive(VtnServiceJsonConsts.VLANID)
							.getAsInt(), VtnServiceJsonConsts.VAL_1,
							VtnServiceJsonConsts.VAL_4095);
				}
			}
			// validation for key:tagged(optional)
			if (isValid) {
				setInvalidParameter(VtnServiceJsonConsts.TAGGED);
				if (portMap.has(VtnServiceJsonConsts.TAGGED)
						&& portMap.getAsJsonPrimitive(
								VtnServiceJsonConsts.TAGGED).getAsString() != null) {
					final String portMp = portMap
							.getAsJsonPrimitive(VtnServiceJsonConsts.TAGGED)
							.getAsString().trim();
					isValid = portMp
							.equalsIgnoreCase(VtnServiceJsonConsts.TRUE)
							|| portMp
									.equalsIgnoreCase(VtnServiceJsonConsts.FALSE);
				}
			}
		}
		LOG.trace("Complete PortMapResourceValidator#validatePut()");
		return isValid;
	}
}