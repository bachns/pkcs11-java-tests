# Tests for SoftHSM using java
   
## Environment variables

```shell
export SOFTHSM2_CONF=/etc/softhsm/softhsm2.conf
export HSM_CONF=/etc/softhsm/pkcs11.cfg
export HSM_KEY_ALIAS=my-key
export HSM_KEY_PASS=123456
export HSM_TOKEN_PASS=123456
```

## Build and test

```shell
mvn clean test
```