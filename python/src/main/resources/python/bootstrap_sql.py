# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Setup SQL over Pandas DataFrames
# It requires next dependencies to be installed:
#  - pandas
#  - pandasql

from __future__ import print_function

try:
  from duckdb import sql
  pysqldf = lambda q: sql(q).df()
except ImportError:
  pysqldf = lambda q: print("Can not run SQL over Pandas DataFrame" +
                              "Make sure 'pandas' and 'pandasql' libraries are installed")

