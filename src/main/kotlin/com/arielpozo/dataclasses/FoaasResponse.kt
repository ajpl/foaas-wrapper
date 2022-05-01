package com.arielpozo.dataclasses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FoaasResponse(val message: String = "", val subtitle: String = "")