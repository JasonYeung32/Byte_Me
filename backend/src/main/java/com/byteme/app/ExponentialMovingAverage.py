import pandas as pd
import random
from datetime import datetime, timedelta
import matplotlib.pyplot as plt
import io
import base64


# takes reserved_at column and returns a count of reservations per day
def countDateDataFrame(dataFrame):
    dateDataFrame = dataFrame.copy(deep=True)

    # convert Instant to date
    dateDataFrame["reserved_at"] = pd.to_datetime(dataFrame["reserved_at"])
    dateDataFrame["date"] = dateDataFrame["reserved_at"].dt.date

    # create a count of reservations grouped by date
    countDateDataFrame = dateDataFrame.groupby("date").size().reset_index(name="reservations")

    return countDateDataFrame

# takes date and count data and generates an EMA by date
def exponentialMovingAverage(dataFrame):
    EMADataFrame = dataFrame.copy(deep=True)
    
    # generate the EMA column
    EMADataFrame["average_reservations"] = EMADataFrame["reservations"].ewm(span=7).mean()

    return EMADataFrame

#plots a time-series dataframe onto a graph and returns it in base64
def graph(dataFrame):
    # creates the graph
    plt.plot(dataFrame["date"], dataFrame["average_reservations"])

    #encodes it in base64
    buffer = io.BytesIO()
    plt.savefig(buffer, format="png")
    buffer.seek(0)

    image = base64.b64encode(buffer.read()).decode("utf-8")

    return image

def predictNextDayEMA(dataFrame):
    #collects the last two weekly averages
    lastWeekCount = dataFrame.iloc[-1]["average_reservations"]
    secondLastWeekCount = dataFrame.iloc[-2]["average_reservations"]

    # calculates the average, daily change in reservation numbers over the week
    gradient = (lastWeekCount - secondLastWeekCount) / 7

    # returns yesterdays average plus the predicted sales change
    return round(lastWeekCount + gradient)

# random generation of data for now
base_date = datetime.now()
reservations = [
    {'reserved_at': (base_date - timedelta(
        days=random.randint(0, 30),
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59),
        seconds=random.randint(0, 59)
    )).isoformat() + 'Z'}
    for _ in range(100)
]

df = countDateDataFrame(pd.DataFrame(reservations))
ema = exponentialMovingAverage(df)
print(graph(ema))
print(predictNextDayEMA(ema))