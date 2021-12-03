package org.helllabs.android.xmp.model

// Same as libxmp/include/xmp.h -> xmp_test_info
// jni testModule() must point to this package
class ModInfo {
    lateinit var name: String
    lateinit var type: String
}

data class ModInfoWithPath(
    var name: String,
    var type: String,
    var path: String,
)
