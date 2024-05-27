import pandas as pd
import json
import joblib
import sklearn
from os.path import dirname, join

def preprocess_single_entry(file):
    df = pd.read_csv(file, sep=';')

    # Drop unnecessary and outdated columns
    deleted_columns = ['label','locationDescription','people','timeStampNetwork', 'timeStampGPS', 'locationDescription', 'people', 'latitudeGPS', 'longitudeGPS', 'latitudeNetwork', 'longitudeNetwork', 'minCn0GPS', 'maxCn0GPS', 'meanCn0GPS', 'minCn0Bluetooth', 'maxCn0Bluetooth', 'minCn0Wifi', 'maxCn0Wifi', 'meanCn0Wifi', 'bAccuracyNetwork', 'speedAccuracyNetwork', 'cellType', 'networkLocationType', 'hAccuracyNetwork', 'vAccuracyNetwork', 'speedAccuracyNetwork', 'bAccuracyNetwork', 'nrBlDevices', 'hAccuracyGPS', 'minCn0Bl', 'meanCn0Bl','maxCn0Bl', 'bAccuracyGPS', 'speedAccuracyGPS', 'vAccuracyGPS', 'nrWifiDevices']
    #Drop label column
    df = safe_delete(df, deleted_columns)

    # Load satellites json
    df['satellites'] = df['satellites'].apply(lambda x: json.loads(x))

    # Add cn0 column for easier computation of statistics
    df['satellite_cn0'] = df['satellites'].apply(lambda x: [sat['cn0'] for sat in x])

    # Calculate min, max, mean. median, mode, variance, standard deviation and range of the satellite cn0
    df['satellite_cn0_min'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).min() if not pd.Series(x).empty else 0)
    df['satellite_cn0_max'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).max() if not pd.Series(x).empty else 0)
    df['satellite_cn0_mean'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).mean() if not pd.Series(x).empty else 0)
    df['satellite_cn0_median'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).median() if not pd.Series(x).empty else 0)
    df['satellite_cn0_mode'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).mode()[0] if not pd.Series(x).mode().empty else 0)
    df['satellite_cn0_std'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).std() if not pd.Series(x).empty else 0)
    df['satellite_cn0_range'] = df['satellite_cn0'].apply(lambda x: pd.Series(x).max() - pd.Series(x).min() if not pd.Series(x).empty else 0)

    # load the bluetooth json and load rssi into a new column
    df['bluetoothDevices'] = df['bluetoothDevices'].apply(lambda x: json.loads(x))
    df['bluetooth_rssi'] = df['bluetoothDevices'].apply(lambda x: [device['rssi'] for device in x])

    # Calculate statistical figures for the bluetooth devices
    df['bluetooth_rssi_min'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).min() if not pd.Series(x).empty else 0)
    df['bluetooth_rssi_max'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).max() if not pd.Series(x).empty else 0)
    df['bluetooth_rssi_mean'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).mean() if not pd.Series(x).empty else 0)
    df['bluetooth_rssi_median'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).median() if not pd.Series(x).empty else 0)
    df['bluetooth_rssi_mode'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).mode()[0] if not pd.Series(x).mode().empty else 0)
    df['bluetooth_rssi_std'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).std() if not pd.Series(x).empty else 0)
    df['bluetooth_rssi_range'] = df['bluetooth_rssi'].apply(lambda x: pd.Series(x).max() - pd.Series(x).min() if not pd.Series(x).empty else 0)

    # load the wifi json and load rssi into a new column
    df['wifiDevices'] = df['wifiDevices'].apply(lambda x: json.loads(x))
    df['wifi_rssi'] = df['wifiDevices'].apply(lambda x: [device['level'] for device in x])

    # Calculate statistical figures for the wifi devices
    df['wifi_rssi_min'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).min() if not pd.Series(x).empty else 0)
    df['wifi_rssi_max'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).max() if not pd.Series(x).empty else 0)
    df['wifi_rssi_mean'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).mean() if not pd.Series(x).empty else 0)
    df['wifi_rssi_median'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).median() if not pd.Series(x).empty else 0)
    df['wifi_rssi_mode'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).mode()[0] if not pd.Series(x).mode().empty else 0)
    df['wifi_rssi_range'] = df['wifi_rssi'].apply(lambda x: pd.Series(x).max() - pd.Series(x).min() if not pd.Series(x).empty else 0)

    # Drop list columns
    df.drop(columns=['satellites', 'bluetoothDevices', 'wifiDevices', 'satellite_cn0', 'bluetooth_rssi', 'wifi_rssi'], axis=1, inplace=True)

    # Check all columns, if emtpy then fill missing columns with 0
    for column in df.columns:
        if df[column].isnull().all():
            df[column] = 0

    return df

def safe_delete(df, columns):
    for column in columns:
        if column in df.columns:
            df.drop(column, axis=1, inplace=True)
    return df

def predict_rfc(df):
    # Load the model
    path = join(dirname(__file__),'random_forest_classifier.joblib')
    model = joblib.load(path)
    # Predict
    prediction = model.predict_proba(df)
    return prediction.tolist()

# For LSTM we only need 1D array of the float values
def preprocess_lstm(file):
    preprocessed = preprocess_single_entry(file)
    return preprocessed.values.flatten()

