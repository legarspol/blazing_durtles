/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model class used to update a study material record for the API.
 *
 * `@JvmOverloads` gives Java callers (`new ApiUpdateStudyMaterial()` in
 * [com.smouldering_durtles.wk.tasks.SubmitStudyMaterialTask], out of scope for this port) a
 * visible no-arg constructor. Callers mutate the nested [studyMaterial] through its own setters
 * rather than replacing it wholesale, so this class doesn't need a setter of its own.
 */
@Serializable
data class ApiUpdateStudyMaterial @JvmOverloads constructor(
    @SerialName("study_material") val studyMaterial: ApiStudyMaterial = ApiStudyMaterial()
)
