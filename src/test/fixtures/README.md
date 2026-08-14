# Precompiled compatibility fixtures

Sources below this directory document binary consumers compiled against the untouched `1.7.12` reference dev jar. They are not part of a Gradle source set and must not be recompiled against the current project during normal tests.

`ReferenceScalaPartialOcclusion.scala` was compiled with Scala 2.11.5 against reference revision `f10595d062bfc1e6dd57a5320fb5b48c383a4f38` and dev jar SHA-256 `bc6e06a01ca8a50b7532aa7d173895c9e89bd2989e43ed977e9eddd3adba57a5`. Its class-file SHA-256 is `c29f2fe296d0a50352b1de1d12ab7a95ac75848a14352ddae06b7aa9640d404a`.

The class is stored as `src/test/resources/compat/ReferenceScalaPartialOcclusion.class.b64` so the repository retains a fixed binary compiled against the reference API. The test decodes and defines that class directly; compiling the documented source against the port would invalidate the compatibility check.
