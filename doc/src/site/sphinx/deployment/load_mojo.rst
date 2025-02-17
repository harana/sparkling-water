Importing H2O Mojo
------------------

H2O Mojo can be imported to Sparkling Water from all data sources supported by Apache Spark such as local file, S3 or HDFS and the
semantics of the import is the same as in the Spark API.


If HDFS is not available for Spark, then call, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    val model = H2OMOJOModel.createFromMojo("prostate.mojo")

or in Python:

.. code:: python

    from pysparkling.ml import *
    model = H2OMOJOModel.createFromMojo("prostate.mojo")

attempts to load the mojo file with the specified name from the current working directory.
You can also specify the full path such as, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    val model = H2OMOJOModel.createFromMojo("/Users/peter/prostate.mojo")

or in Python:

.. code:: python

    from pysparkling.ml import *
    model = H2OMOJOModel.createFromMojo("/Users/peter/prostate.mojo")


In the case Spark is running on Hadoop and HDFS is available, then call, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    val model = H2OMOJOModel.createFromMojo("prostate.mojo")

or in Python:

.. code:: python

    from pysparkling.ml import *
    model = H2OMOJOModel.createFromMojo("prostate.mojo")


attempts to load the mojo from the HDFS home directory of the current user.
You can also specify the absolute path in this case as, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    val model = H2OMOJOModel.createFromMojo("/user/peter/prostate.mojo")

or in Python:

.. code:: python

    from pysparkling.ml import *
    model = H2OMOJOModel.createFromMojo("/user/peter/prostate.mojo")


Both calls load the mojo file from the following location ``hdfs://{server}:{port}/user/peter/prostate.mojo``, where ``{server}`` and ``{port}`` is automatically filled in by Spark.


You can also manually specify the type of data source you need to use, in that case, you need to provide the schema, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    // HDFS
    val modelHDFS = H2OMOJOModel.createFromMojo("hdfs:///user/peter/prostate.mojo")
    // Local file
    val modelLocal = H2OMOJOModel.createFromMojo("file:///Users/peter/prostate.mojo")

or in Python:

.. code:: python

    from pysparkling.ml import *
    # HDFS
    modelHDFS = H2OMOJOModel.createFromMojo("hdfs:///user/peter/prostate.mojo")
    # Local file
    modelLocal = H2OMOJOModel.createFromMojo("file:///Users/peter/prostate.mojo")


The loaded model is an immutable instance, so it's not possible to change the configuration of the model during its existence.
On the other hand, the model can be configured during its creation via ``H2OMOJOSettings``, in Scala:

.. code:: scala

    import org.apache.spark.ml.h2o.models._
    val settings = H2OMOJOSettings(convertUnknownCategoricalLevelsToNa = true, convertInvalidNumbersToNa = true)
    val model = H2OMOJOModel.createFromMojo("prostate.mojo", settings)

or in Python:

.. code:: python

    from pysparkling.ml import *
    settings = H2OMOJOSettings(convertUnknownCategoricalLevelsToNa = True, convertInvalidNumbersToNa = True)
    model = H2OMOJOModel.create_from_mojo("prostate.mojo", settings)