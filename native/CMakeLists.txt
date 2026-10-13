cmake_minimum_required(VERSION 3.25)
project(mc_native_chunk_engine LANGUAGES C)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)
# Phase 0 only establishes build tooling. No binary or JNI ABI is advertised.
# Scalar implementation and sanitized tests are gated on Phase 3 profiling.
