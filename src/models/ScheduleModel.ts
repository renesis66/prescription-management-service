import AWS from 'aws-sdk';
import { PrescriptionSchedule, DynamoDBItem } from '../types';

export class ScheduleModel {
  private dynamodb: AWS.DynamoDB.DocumentClient;
  private tableName: string;

  constructor() {
    this.dynamodb = new AWS.DynamoDB.DocumentClient();
    this.tableName = process.env.PRESCRIPTION_SCHEDULES_TABLE || 'prescription-schedules';
  }

  async createScheduleEntry(schedule: PrescriptionSchedule): Promise<PrescriptionSchedule> {
    const scheduleItem: PrescriptionSchedule & DynamoDBItem = {
      PK: `PRESCRIPTION#${schedule.prescriptionId}`,
      SK: `SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}`,
      GSI1PK: `PATIENT#${schedule.patientId}`,
      GSI1SK: `SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}`,
      ...schedule
    };

    await this.dynamodb.put({
      TableName: this.tableName,
      Item: scheduleItem
    }).promise();

    const { PK, SK, GSI1PK, GSI1SK, ...scheduleData } = scheduleItem;
    return scheduleData;
  }

  async getScheduleByPrescription(prescriptionId: string): Promise<PrescriptionSchedule[]> {
    const params = {
      TableName: this.tableName,
      KeyConditionExpression: 'PK = :pk',
      ExpressionAttributeValues: {
        ':pk': `PRESCRIPTION#${prescriptionId}`
      }
    };

    const result = await this.dynamodb.query(params).promise();
    return result.Items?.map(item => {
      const { PK, SK, GSI1PK, GSI1SK, ...schedule } = item;
      return schedule as PrescriptionSchedule;
    }) || [];
  }

  async getPatientSchedule(patientId: string, date?: string): Promise<PrescriptionSchedule[]> {
    const params: AWS.DynamoDB.DocumentClient.QueryInput = {
      TableName: this.tableName,
      IndexName: 'GSI1',
      KeyConditionExpression: date 
        ? 'GSI1PK = :gsi1pk AND begins_with(GSI1SK, :gsi1sk)'
        : 'GSI1PK = :gsi1pk AND begins_with(GSI1SK, :prefix)',
      ExpressionAttributeValues: {
        ':gsi1pk': `PATIENT#${patientId}`,
        ...(date 
          ? { ':gsi1sk': `SCHEDULE#${date}` }
          : { ':prefix': 'SCHEDULE#' }
        )
      }
    };

    const result = await this.dynamodb.query(params).promise();
    return result.Items?.map(item => {
      const { PK, SK, GSI1PK, GSI1SK, ...schedule } = item;
      return schedule as PrescriptionSchedule;
    }) || [];
  }

  async updateScheduleStatus(
    prescriptionId: string, 
    scheduledDate: string, 
    scheduledTime: string, 
    status: 'PENDING' | 'TAKEN' | 'MISSED'
  ): Promise<PrescriptionSchedule | null> {
    const params = {
      TableName: this.tableName,
      Key: {
        PK: `PRESCRIPTION#${prescriptionId}`,
        SK: `SCHEDULE#${scheduledDate}#${scheduledTime}`
      },
      UpdateExpression: 'SET #status = :status',
      ExpressionAttributeNames: {
        '#status': 'status'
      },
      ExpressionAttributeValues: {
        ':status': status
      },
      ReturnValues: 'ALL_NEW' as const
    };

    const result = await this.dynamodb.update(params).promise();
    if (!result.Attributes) {
      return null;
    }

    const { PK, SK, GSI1PK, GSI1SK, ...schedule } = result.Attributes;
    return schedule as PrescriptionSchedule;
  }

  async deleteScheduleByPrescription(prescriptionId: string): Promise<void> {
    const schedules = await this.getScheduleByPrescription(prescriptionId);
    
    if (schedules.length === 0) {
      return;
    }

    const deleteRequests = schedules.map(schedule => ({
      DeleteRequest: {
        Key: {
          PK: `PRESCRIPTION#${schedule.prescriptionId}`,
          SK: `SCHEDULE#${schedule.scheduledDate}#${schedule.scheduledTime}`
        }
      }
    }));

    for (let i = 0; i < deleteRequests.length; i += 25) {
      const batch = deleteRequests.slice(i, i + 25);
      await this.dynamodb.batchWrite({
        RequestItems: {
          [this.tableName]: batch
        }
      }).promise();
    }
  }
}