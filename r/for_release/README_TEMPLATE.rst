RSparkling
==========

|Join the chat at https://gitter.im/h2oai/rsparkling|
|License| |CRAN| |Downloads| |Powered by H2O.ai|

The **rsparkling** R package is an extension package for `sparklyr <http://spark.rstudio.com>`__
that creates an R front-end for the `Sparkling Water <https://www.h2o.ai/sparkling-water/>`__
package from `H2O <https://www.h2o.ai/>`__.
This provides an interface to H2O's high performance, distributed machine learning algorithms on
Spark, using R.

This package implements basic functionality (creating an H2OContext, showing the H2O Flow
interface, and converting between Spark DataFrames and H2O Frames). The main purpose of
this package is to provide a connector between sparklyr and H2O's machine learning algorithms.

The **rsparkling** package uses **sparklyr** for Spark job deployment and initialization
of Sparkling Water. After that, user can use the regular **h2o** R package for modeling. In the
following sections we show how to install each of these packages.

Installation of SparklyR & Spark
--------------------------------

Install sparklyr
~~~~~~~~~~~~~~~~
We recommend the latest stable version of `sparklyr <http://spark.rstudio.com/index.html>`__.

.. code:: r

   install.packages("sparklyr")

Install Spark via sparklyr
~~~~~~~~~~~~~~~~~~~~~~~~~~
The **sparklyr** package makes it easy to install any particular version of Spark.
Prior to installing **h2o** and **rsparkling**, the user will need to decide which version of
Spark they would like to work with, as the remaining installation revolve around a particular major
version of Spark (2.1, 2.2, 2.3 or 2.4).

The following command will install Spark SUBST_SPARK_VERSION:

.. code:: r

   library(sparklyr)
   spark_install(version = "SUBST_SPARK_VERSION")

**NOTE**: The previous command requires access to the internet. If you are not connected to the
internet/behind a firewall you would need to do the following:


1. Download `Spark <https://spark.apache.org/downloads.html>`__ (Pick the major version that corresponds to Sparkling Water)
2. Unzip Spark files
3. Set the ``SPARK_HOME`` environment variable to the location of the downloaded Spark folder in R as follows:

   .. code:: r

      Sys.setenv(SPARK_HOME="/path/to/spark")

Install H2O
-----------


H2O & Sparkling Water Versions Mapping
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
**rsparkling** requires certain version of H2O to be used depending on desired Sparkling Water & Spark versions.
This is because each release of Sparking Water is built from specific versions of H2O.

By default, **rsparkling** automatically uses the latest Sparkling Water based on the major Spark version provided
and advices the user which H2O version to install.

Advanced users may want to choose a particular Sparking Water / H2O version (specific Sparkling Water
versions must match specific Spark and H2O versions).

SUBST_RSPARKLING_RELEASE_TABLE


**NOTE**: A call to ``rsparkling::h2o_release_table()`` displays the release table in your R console and returns
a ``data.frame`` containing this information.

Prepare Environment for H2O Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
It is advised to remove previously installed H2O versions and install H2O dependencies. The command bellow
can be used for this.

.. code:: r

   # The following two commands remove any previously installed H2O packages for R.
   if ("package:h2o" %in% search()) { detach("package:h2o", unload=TRUE) }
   if ("h2o" %in% rownames(installed.packages())) { remove.packages("h2o") }

   # Install packages H2O depends on
   pkgs <- c("methods", "statmod", "stats", "graphics", "RCurl", "jsonlite", "tools", "utils")
   for (pkg in pkgs) {
       if (! (pkg %in% rownames(installed.packages()))) { install.packages(pkg) }
   }

Install H2O from CRAN
~~~~~~~~~~~~~~~~~~~~~
In case of installation from CRAN, the typical ``install.packages("h2o", "SUBST_H2O_VERSION")`` command can be used. Please note
that the latest released version might not be available in CRAN. In that case, please install H2O from S3.

Install H2O from S3
~~~~~~~~~~~~~~~~~~~
H2O can be also installed from hosted R repository in H2O's S3 buckets.

At present, you can install the **h2o** R package using a repository URL comprised
of the H2O version name and number. Example: `http://h2o-release.s3.amazonaws.com/h2o/rel-SUBST_H2O_RELEASE_NAME/SUBST_H2O_BUILD_NUMBER/R`

.. code:: r

   # Download, install, and initialize the H2O package for R.
   # In this case we are using rel-SUBST_H2O_RELEASE_NAME SUBST_H2O_BUILD_NUMBER (SUBST_H2O_VERSION)
   install.packages("h2o", type = "source", repos = "http://h2o-release.s3.amazonaws.com/h2o/rel-SUBST_H2O_RELEASE_NAME/SUBST_H2O_BUILD_NUMBER/R")


Install rsparkling
------------------

RSparkling can be installed from hosted R repository in Sparkling Water's S3 buckets
from the link `http://h2o-release.s3.amazonaws.com/sparkling-water/rel-SUBST_SPARK_MAJOR_VERSION/SUBST_SW_MINOR_VERSION/R` as:

.. code:: r

   # Download, install, and initialize the RSparkling
   install.packages("rsparkling", type = "source", repos = "http://h2o-release.s3.amazonaws.com/sparkling-water/rel-SUBST_SPARK_MAJOR_VERSION/SUBST_SW_MINOR_VERSION/R")


RSparkling & SparklyR Configuration
-----------------------------------

Configure Sparkling Water Version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

With no configuration, the latest version of Sparkling Water will be used based on the version of
Spark installed. All the additional `options` configurations needs to be called before `library(rsparkling)`
in order for them to take affect.


Particular version of Sparkling Water can be specified as:

.. code:: r

   options(rsparkling.sparklingwater.version = ...)


In both cases, the internet access is required as correct Sparkling Water versions will be fetched from Maven
central. If you don't have internet access or firewall is set up, you can specify Sparkling Water JAR directly as

.. code:: r

   options(rsparkling.sparklingwater.location = "/path/to/sparkling_water.jar")

This JAR file can be obtain in following steps:

1. Download the Sparkling Water jar of your choice based on the integration table above.
   To do this go to the following link where ``[SW Major Version]`` is the major version of Sparkling Water you wish to use, i.e., ``SUBST_SW_MAJOR_VERSION`` and ``[SW Minor Version]`` is the minor version of Sparkling Water you wish to use, i.e., ``SUBST_SW_MINOR_VERSION``,
   such as ``http://h2o-release.s3.amazonaws.com/sparkling-water/rel-[SW Major Version]/[SW Minor Version]/index.html``
2. Click the ``DOWNLOAD SPARKLING WATER`` tab, which will download a ``.zip`` file of Sparkling Water.
3. Run the following command to unzip the folder:

.. code:: bash

	unzip sparkling-water-[SW Major Version].[SW Minor Version].zip

4. The path to the Sparkling Water jar file is: ``sparkling-water-[SW Major Version].[SW Minor Version]/assembly/build/libs/sparkling-water-assembly_*.jar``.


Configure Spark Connection
~~~~~~~~~~~~~~~~~~~~~~~~~~
Once we've installed **rsparkling** and it's dependencies, the first step would be to create a Spark connection as follows:

.. code:: r

   sc <- spark_connect(master = "local", version = "SUBST_SPARK_VERSION")


**NOTE**: Please be sure to set ``version`` to the proper Spark version utilized by your version of Sparkling Water in ``spark_connect()``

**NOTE**: The previous command requires access to the internet. If you are not connected to the internet/behind a firewall, please
first read the previous section about Spark installation.

``spark_connect`` method has also ``spark_home`` argument which defaults to the ``SPARK_HOME`` environment
variable. If ``SPARK_HOME`` is defined it will be always used unless the ``version``
parameter is specified to force the use of a locally installed version. Therefore, to use existing
Spark, please run:

.. code:: r

	sc <- spark_connect(master = "local")

Changing the Default H2O Client Port
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
RSparkling does not expose setters and getters for specifying configuration options.
You must specify the Spark configuration options directly, for example:

.. code:: r

   config=spark_config()
   config=c(config, list("spark.ext.h2o.node.port.base"="55555", "spark.ext.h2o.client.port.base"="44444"))
   sc <- spark_connect(master="yarn-client", app_name = "demo", config = config)


In the above, ``spark.ext.h2o.node.port.base`` affects the worker nodes,
and ``spark.ext.h2o.client.port.base`` affects the client.

Using RSparkling
================

H2OContext & Flow
-----------------

The call to ``library(rsparkling)`` automatically registers the Sparkling Water extension.
Let's inspect the `H2OContext` for our Spark connection:

.. code:: r

   h2o_context(sc)

       ## <jobj[6]>
       ##   class org.apache.spark.h2o.H2OContext
       ##
       ## Sparkling Water Context:
       ##  * H2O name: sparkling-water-jjallaire_-1482215501
       ##  * number of executors: 1
       ##  * list of used executors:
       ##   (executorId, host, port)
       ##   ------------------------
       ##   (driver,localhost,54323)
       ##   ------------------------
       ##
       ##   Open H2O Flow in browser: http://127.0.0.1:54323 (CMD + click in Mac OSX)
       ##

We can also view the H2O Flow web UI:

.. code:: r

   h2o_flow(sc)


H2O with Spark DataFrames
-------------------------
As an example, let's copy the mtcars dataset to to Spark so we can access it from H2O Sparkling Water:

.. code:: r

   library(dplyr)
   mtcars_tbl <- copy_to(sc, mtcars, overwrite = TRUE)
   mtcars_tbl

      ## Source:   query [?? x 11]
      ## Database: spark connection master=local[8] app=sparklyr local=TRUE
      ##
      ##      mpg   cyl  disp    hp  drat    wt  qsec    vs    am  gear  carb
      ##    <dbl> <dbl> <dbl> <dbl> <dbl> <dbl> <dbl> <dbl> <dbl> <dbl> <dbl>
      ## 1   21.0     6 160.0   110  3.90 2.620 16.46     0     1     4     4
      ## 2   21.0     6 160.0   110  3.90 2.875 17.02     0     1     4     4
      ## 3   22.8     4 108.0    93  3.85 2.320 18.61     1     1     4     1
      ## 4   21.4     6 258.0   110  3.08 3.215 19.44     1     0     3     1
      ## 5   18.7     8 360.0   175  3.15 3.440 17.02     0     0     3     2
      ## 6   18.1     6 225.0   105  2.76 3.460 20.22     1     0     3     1
      ## 7   14.3     8 360.0   245  3.21 3.570 15.84     0     0     3     4
      ## 8   24.4     4 146.7    62  3.69 3.190 20.00     1     0     4     2
      ## 9   22.8     4 140.8    95  3.92 3.150 22.90     1     0     4     2
      ## 10  19.2     6 167.6   123  3.92 3.440 18.30     1     0     4     4
      ## ... with more rows


The use case we'd like to enable is calling the H2O algorithms and feature transformers directly on Spark DataFrames
that we've manipulated with dplyr. This is indeed supported by the Sparkling Water package.
Here is how you convert a Spark DataFrame into an H2O Frame:

.. code:: r

   mtcars_hf <- as_h2o_frame(sc, mtcars_tbl)
   mtcars_hf

      ## <jobj[103]>
      ##   class water.fvec.H2OFrame
      ##   Frame frame_rdd_39 (32 rows and 11 cols):
      ##                        mpg  cyl                disp   hp                drat                  wt                qsec  vs  am  gear  carb
      ##     min               10.4    4                71.1   52                2.76               1.513                14.5   0   0     3     1
      ##    mean          20.090625    6          230.721875  146           3.5965625             3.21725  17.848750000000003   0   0     3     2
      ##  stddev  6.026948052089104    1  123.93869383138194   68  0.5346787360709715  0.9784574429896966  1.7869432360968436   0   0     0     1
      ##     max               33.9    8               472.0  335                4.93               5.424                22.9   1   1     5     8
      ## missing                0.0    0                 0.0    0                 0.0                 0.0                 0.0   0   0     0     0
      ##       0               21.0    6               160.0  110                 3.9                2.62               16.46   0   1     4     4
      ##       1               21.0    6               160.0  110                 3.9               2.875               17.02   0   1     4     4
      ##       2               22.8    4               108.0   93                3.85                2.32               18.61   1   1     4     1
      ##       3               21.4    6               258.0  110                3.08               3.215               19.44   1   0     3     1
      ##       4               18.7    8               360.0  175                3.15                3.44               17.02   0   0     3     2
      ##       5               18.1    6               225.0  105                2.76                3.46               20.22   1   0     3     1
      ##       6               14.3    8               360.0  245                3.21                3.57               15.84   0   0     3     4
      ##       7               24.4    4               146.7   62                3.69                3.19                20.0   1   0     4     2
      ##       8               22.8    4               140.8   95                3.92                3.15                22.9   1   0     4     2
      ##       9               19.2    6               167.6  123                3.92                3.44                18.3   1   0     4     4
      ##      10               17.8    6               167.6  123                3.92                3.44                18.9   1   0     4     4
      ##      11               16.4    8               275.8  180                3.07                4.07                17.4   0   0     3     3
      ##      12               17.3    8               275.8  180                3.07                3.73                17.6   0   0     3     3
      ##      13               15.2    8               275.8  180                3.07                3.78                18.0   0   0     3     3
      ##      14               10.4    8               472.0  205                2.93                5.25               17.98   0   0     3     4
      ##      15               10.4    8               460.0  215                 3.0               5.424               17.82   0   0     3     4
      ##      16               14.7    8               440.0  230                3.23               5.345               17.42   0   0     3     4
      ##      17               32.4    4                78.7   66                4.08                 2.2               19.47   1   1     4     1
      ##      18               30.4    4                75.7   52                4.93               1.615               18.52   1   1     4     2
      ##      19               33.9    4                71.1   65                4.22               1.835                19.9   1   1     4     1


Obtaining Logs
--------------

Look at the Spark log from R:

.. code:: r

   spark_log(sc, n = 100)


Disconnect from Spark
---------------------
Now we disconnect from Spark, this will result in the H2OContext being stopped as well
since it's owned by the spark shell process used by our Spark connection:

.. code:: r

   spark_disconnect(sc)


Machine Learning with RSparkling & H2O
--------------------------------------

Using the same mtcars dataset, here is an example where we train a Gradient Boosting Machine
(GBM) to predict "mpg".

Initialize H2O
~~~~~~~~~~~~~~

.. code:: r

   library(h2o)

Data Preparations
~~~~~~~~~~~~~~~~~

Define the response, `y`, and set of predictor variables, `x`:

.. code:: r

   y <- "mpg"
   x <- setdiff(names(mtcars_hf), y)


Let's split the data into a train and test set using H2O. The ``h2o.splitFrame``
function defaults to a 75-25 split (``ratios = 0.75``), but here we will make a 70-30 train-test split:

.. code:: r

   # Split the mtcars H2O Frame into train & test sets
   splits <- h2o.splitFrame(mtcars_hf, ratios = 0.7, seed = 1)

Model Training
~~~~~~~~~~~~~~
Now train an H2O GBM using the training H2OFrame.

.. code:: r

   fit <- h2o.gbm(x = x,
                  y = y,
                  training_frame = splits[[1]],
                  min_rows = 1,
                  seed = 1)
   print(fit)

      ## H2ORegressionModel: gbm
      ## Model ID:  GBM_model_R_1474763476171_1
      ## Model Summary:
      ##  number_of_trees number_of_internal_trees model_size_in_bytes min_depth
      ##   1              50                       50               14807         5
      ##  max_depth mean_depth min_leaves max_leaves mean_leaves
      ##   1         5    5.00000         17         21    18.64000
      ##
      ##
      ## H2ORegressionMetrics: gbm
      ## ** Reported on training data. **
      ##
      ## MSE:  0.001211724
      ## RMSE:  0.03480983
      ## MAE:  0.02761402
      ## RMSLE:  0.001929304
      ## Mean Residual Deviance :  0.001211724

Model Performance:
~~~~~~~~~~~~~~~~~~

We can evaluate the performance of the GBM by evaluating its performance on a test set.

.. code:: r

   perf <- h2o.performance(fit, newdata = splits[[2]])
   print(perf)

      ## H2ORegressionMetrics: gbm
      ##
      ## MSE:  2.707001
      ## RMSE:  1.645297
      ## MAE:  1.455267
      ## RMSLE:  0.08579109
      ## Mean Residual Deviance :  2.707001



Predictions
~~~~~~~~~~~

To generate predictions on a test set, you do the following.
This will return an H2OFrame with a single (or multiple) columns of predicted values.
If regression, it will be a single colum, if binary classification it will be 3 columns
and in multi-class prediction it will be C+1 columns (where C is the number of classes).

.. code:: r

   pred_hf <- h2o.predict(fit, newdata = splits[[2]])
   head(pred_hf)

      ##   predict
      ## 1 21.39512
      ## 2 16.92804
      ## 3 15.19558
      ## 4 20.47695
      ## 5 20.47695
      ## 6 15.24433



Now let's say you want to make this H2OFrame available to Spark. You can convert an H2OFrame into a Spark DataFrame using the ``as_spark_dataframe`` function:

.. code:: r

   pred_sdf <- as_spark_dataframe(sc, pred_hf)
   head(pred_sdf)

      Source:   query [?? x 1]
      Database: spark connection master=local[8] app=sparklyr local=TRUE

      ##   predict
      ##   <dbl>
      ## 1 21.39512
      ## 2 16.92804
      ## 3 15.19558
      ## 4 20.47695
      ## 5 20.47695
      ## 6 15.24433


Additional Resources
--------------------

- `Main documentation site <http://docs.h2o.ai>`__
- `H2O.ai website <http://h2o.ai>`__
- `Example code <https://github.com/h2oai/rsparkling/blob/master/inst/examples/example_rsparkling.R>`__
- `Troubleshooting RSparkling on Windows <http://docs.h2o.ai/sparkling-water/master/bleeding-edge/doc/deployment/rsparkling_on_windows.html>`__

If you are new to H2O for machine learning, we recommend you start with:

- `Intro to H2O Tutorial <https://github.com/h2oai/h2o-tutorials/blob/master/h2o-open-tour-2016/chicago/intro-to-h2o.R>`__
- `H2O Grid Search & Model Selection Tutorial <https://github.com/h2oai/h2o-tutorials/blob/master/h2o-open-tour-2016/chicago/grid-search-model-selection.R>`__

There is also number of other H2O R `tutorials <https://github.com/h2oai/h2o-tutorials>`__, `demos <https://github.com/h2oai/h2o-3/tree/master/h2o-r/demos>`__ available, and the `Machine Learning with R and
H2O Booklet (pdf) <http://docs.h2o.ai/h2o/latest-stable/h2o-docs/booklets/RBooklet.pdf>`__.


.. |Join the chat at https://gitter.im/h2oai/rsparkling| image:: https://badges.gitter.im/Join%20Chat.svg
   :target: Join the chat at https://gitter.im/h2oai/rsparkling?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
.. |License| image:: https://img.shields.io/badge/License-Apache%202-blue.svg
   :target: LICENSE
.. |CRAN| image:: http://www.r-pkg.org/badges/version/rsparkling
   :target: https://cran.r-project.org/package=rsparkling
.. |Downloads| image:: http://cranlogs.r-pkg.org/badges/rsparkling?color=brightgreen
   :target: http://www.r-pkg.org/pkg/rsparkling
.. |Powered by H2O.ai| image:: https://img.shields.io/badge/powered%20by-h2oai-yellow.svg
   :target: https://github.com/h2oai/
.. |H2O| replace:: H\ :sub:`2`\ O
